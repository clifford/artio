/*
 * Copyright 2015 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.fix_gateway;

import uk.co.real_logic.aeron.Aeron;
import uk.co.real_logic.aeron.Subscription;
import uk.co.real_logic.aeron.logbuffer.BufferClaim;
import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.concurrent.*;
import uk.co.real_logic.fix_gateway.framer.Framer;
import uk.co.real_logic.fix_gateway.framer.FramerCommand;
import uk.co.real_logic.fix_gateway.framer.FramerProxy;
import uk.co.real_logic.fix_gateway.framer.Multiplexer;
import uk.co.real_logic.fix_gateway.logger.*;
import uk.co.real_logic.fix_gateway.replication.ReplicationStreams;
import uk.co.real_logic.fix_gateway.session.InitiatorSession;
import uk.co.real_logic.fix_gateway.session.SessionIdStrategy;
import uk.co.real_logic.fix_gateway.session.SessionIds;
import uk.co.real_logic.fix_gateway.util.MilliClock;

import java.nio.channels.ClosedByInterruptException;
import java.util.Arrays;
import java.util.List;

public class FixGateway implements AutoCloseable
{
    public static final int INBOUND_DATA_STREAM = 0;
    public static final int INBOUND_CONTROL_STREAM = 1;
    public static final int OUTBOUND_DATA_STREAM = 2;
    public static final int OUTBOUND_CONTROL_STREAM = 3;

    private CountersFile countersFile;
    private FixCounters fixCounters;

    private Aeron aeron;
    private ReplicationStreams inboundStreams;
    private ReplicationStreams outboundStreams;

    private FramerProxy framerProxy;

    private AgentRunner framerRunner;
    private AgentRunner loggingRunner;

    private final Signal signal = new Signal();
    private final long connectionTimeout;

    private InitiatorSession addedSession;
    private Exception exception;

    FixGateway(final StaticConfiguration configuration)
    {
        connectionTimeout = configuration.connectionTimeout();

        countersFile = new CountersFile(true, configuration);
        fixCounters = new FixCounters(countersFile.createCountersManager());

        initAeron();
        initReplicationStreams(configuration);
        initFramer(configuration, fixCounters);
        initLogger(configuration);
    }

    private void initLogger(final StaticConfiguration configuration)
    {
        final int loggerCacheCapacity = configuration.loggerCacheCapacity();
        final String logFileDir = configuration.logFileDir();

        final Archiver archiver = new Archiver(
            LoggerUtil::map, outboundStreams, LoggerUtil.newArchiveMetaData(configuration), logFileDir, loggerCacheCapacity);
        final ArchiveReader archiveReader = new ArchiveReader(
            LoggerUtil::mapExistingFile, LoggerUtil.newArchiveMetaData(configuration), logFileDir, loggerCacheCapacity);

        final List<Index> indices = Arrays.asList(
            new ReplayIndex(logFileDir, configuration.indexFileSize(), loggerCacheCapacity, LoggerUtil::map));
        final Indexer indexer = new Indexer(indices, outboundStreams);

        final ReplayQuery replayQuery = new ReplayQuery(
            logFileDir, loggerCacheCapacity, LoggerUtil::mapExistingFile, archiveReader);
        final Replayer replayer = new Replayer(
            inboundStreams.dataSubscription(),
            replayQuery,
            outboundStreams.dataPublication(),
            new BufferClaim(),
            backoffIdleStrategy());

        final Agent loggingAgent = new CompositeAgent(archiver, new CompositeAgent(indexer, replayer));

        loggingRunner =
            new AgentRunner(backoffIdleStrategy(), Throwable::printStackTrace, fixCounters.exceptions(), loggingAgent);
    }

    private void initFramer(final StaticConfiguration configuration, final FixCounters fixCounters)
    {
        final SequencedContainerQueue<FramerCommand> framerCommands = new ManyToOneConcurrentArrayQueue<>(10);
        framerProxy = new FramerProxy(framerCommands, fixCounters.framerProxyFails(), backoffIdleStrategy());

        final SessionIds sessionIds = new SessionIds();

        final IdleStrategy idleStrategy = backoffIdleStrategy();
        final Multiplexer multiplexer = new Multiplexer();
        final Subscription dataSubscription = outboundStreams.dataSubscription();
        final SessionIdStrategy sessionIdStrategy = configuration.sessionIdStrategy();

        final MilliClock systemClock = System::currentTimeMillis;

        final ConnectionHandler handler = new ConnectionHandler(
            systemClock,
            configuration,
            sessionIdStrategy,
            sessionIds,
            inboundStreams,
            outboundStreams,
            idleStrategy,
            fixCounters);

        final Framer framer = new Framer(systemClock, configuration, handler, framerCommands,
            multiplexer, this, dataSubscription);
        multiplexer.framer(framer);
        framerRunner = new AgentRunner(idleStrategy, Throwable::printStackTrace, fixCounters.exceptions(), framer);
    }

    private void initReplicationStreams(final StaticConfiguration configuration)
    {
        final String channel = configuration.aeronChannel();

        inboundStreams = new ReplicationStreams(
            channel, aeron, fixCounters.failedInboundPublications(), INBOUND_DATA_STREAM, INBOUND_CONTROL_STREAM);
        outboundStreams = new ReplicationStreams(
            channel, aeron, fixCounters.failedOutboundPublications(), OUTBOUND_DATA_STREAM, OUTBOUND_CONTROL_STREAM);
    }

    private void initAeron()
    {
        final Aeron.Context ctx = new Aeron.Context();
        ctx.errorHandler(throwable ->
        {
            if (!(throwable instanceof ClosedByInterruptException))
            {
                Aeron.DEFAULT_ERROR_HANDLER.onError(throwable);
            }
        });
        aeron = Aeron.connect(ctx);
    }

    private BackoffIdleStrategy backoffIdleStrategy()
    {
        return new BackoffIdleStrategy(1, 1, 1, 1 << 20);
    }

    public static FixGateway launch(final StaticConfiguration configuration)
    {
        return new FixGateway(configuration.conclude()).start();
    }

    private FixGateway start()
    {
        start(framerRunner);
        start(loggingRunner);
        return this;
    }

    private void start(final AgentRunner runner)
    {
        final Thread thread = new Thread(runner);
        thread.setName(runner.agent().roleName());
        thread.start();
    }

    // TODO: figure out correct type for dictionary
    public synchronized InitiatorSession initiate(final SessionConfiguration configuration)
    {
        framerProxy.connect(configuration);
        signal.await(connectionTimeout);
        final InitiatorSession addedSession = this.addedSession;
        if (addedSession == null)
        {
            LangUtil.rethrowUnchecked(this.exception != null ? this.exception : timeout(configuration));
        }
        this.addedSession = null;
        return addedSession;
    }

    private ConnectionTimeoutException timeout(final SessionConfiguration configuration)
    {
        return new ConnectionTimeoutException(
            "Connection timed out connecting to: " + configuration.host() + ":" + configuration.port());
    }

    public synchronized void close() throws Exception
    {
        framerRunner.close();
        loggingRunner.close();

        inboundStreams.close();
        outboundStreams.close();
        aeron.close();
        countersFile.close();
    }

    public void onInitiatorSessionActive(final InitiatorSession session)
    {
        addedSession = session;
        signal.signal();
    }

    public void onInitiationError(final Exception exception)
    {
        this.exception = exception;
        signal.signal();
    }
}
