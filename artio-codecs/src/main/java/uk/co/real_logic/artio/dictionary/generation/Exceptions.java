/*
 * Copyright 2013 Real Logic Ltd.
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
package uk.co.real_logic.artio.dictionary.generation;

import org.agrona.LangUtil;
import org.agrona.concurrent.Agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public final class Exceptions
{

    /**
     * Close all closeables in closeables. If any of them throw then throw that exception.
     * If multiple closeables throw an exception when being closed, then throw an exception that contains
     * all of them as suppressed exceptions.
     *
     * @param closeables to be closed.
     */
    public static void closeAll(final List<? extends AutoCloseable> closeables)
    {
        if (closeables == null)
        {
            return;
        }

        final List<Exception> exceptions = new ArrayList<>();
        for (final AutoCloseable closeable : closeables)
        {
            if (closeable != null)
            {
                try
                {
                    closeable.close();
                }
                catch (final Exception ex)
                {
                    exceptions.add(ex);
                }
            }
        }

        if (!exceptions.isEmpty())
        {
            final Exception exception = exceptions.remove(0);
            exceptions.forEach(exception::addSuppressed);
            LangUtil.rethrowUnchecked(exception);
        }
    }

    public static void closeAll(final AutoCloseable... closeables)
    {
        closeAll(Arrays.asList(closeables));
    }

    public static void closeAll(final Agent... agents)
    {
        closeAll(Stream
            .of(agents)
            .filter(Objects::nonNull)
            .map((agent) -> (AutoCloseable)agent::onClose)
            .collect(toList()));
    }

    public static void suppressingClose(final AutoCloseable closeable, final Exception originalException)
    {
        try
        {
            closeable.close();
        }
        catch (final Exception ex)
        {
            originalException.addSuppressed(ex);
        }
    }

    public static void printStackTrace()
    {
        try
        {
            throw new Exception();
        }
        catch (final Exception ex)
        {
            ex.printStackTrace(System.out);
        }
    }

    public static boolean isJustDisconnect(final Exception ex)
    {
        final String msg = ex.getMessage();
        return msg.contains("Connection reset by peer") ||
               msg.contains("An established connection was aborted");
    }
}
