// Copyright (C) 2001 - 2008 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.engine.process;

import net.grinder.common.FilenameFactory;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.script.InternalScriptContext;
import net.grinder.script.Statistics;
import net.grinder.script.SSLControl;
import net.grinder.script.TestRegistry;
import net.grinder.util.Sleeper;


/**
 * Implementation of <code>ScriptContext</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class ScriptContextImplementation implements InternalScriptContext {

  private final WorkerIdentity m_workerIdentity;
  private final ThreadContextLocator m_threadContextLocator;
  private final GrinderProperties m_properties;
  private final Logger m_logger;
  private final FilenameFactory m_filenameFactory;
  private final Sleeper m_sleeper;
  private final SSLControl m_sslControl;
  private final Statistics m_scriptStatistics;
  private final TestRegistry m_testRegistry;
  private final ThreadStarter m_threadStarter;

  public ScriptContextImplementation(WorkerIdentity workerIdentity,
                                     ThreadContextLocator threadContextLocator,
                                     GrinderProperties properties,
                                     Logger logger,
                                     FilenameFactory filenameFactory,
                                     Sleeper sleeper,
                                     SSLControl sslControl,
                                     Statistics scriptStatistics,
                                     TestRegistry testRegistry,
                                     ThreadStarter threadStarter) {
    m_workerIdentity = workerIdentity;
    m_threadContextLocator = threadContextLocator;
    m_properties = properties;
    m_logger = logger;
    m_filenameFactory = filenameFactory;
    m_sleeper = sleeper;
    m_sslControl = sslControl;
    m_scriptStatistics = scriptStatistics;
    m_testRegistry = testRegistry;
    m_threadStarter = threadStarter;
  }

  public int getAgentNumber() {
    return m_workerIdentity.getAgentIdentity().getNumber();
  }

  public String getProcessName() {
    return m_workerIdentity.getName();
  }

  public int getProcessNumber() {
    return m_workerIdentity.getNumber();
  }

  public int getThreadNumber() {
    final ThreadContext threadContext = m_threadContextLocator.get();

    if (threadContext != null) {
      return threadContext.getThreadNumber();
    }

    return -1;
  }

  public int getRunNumber() {
    final ThreadContext threadContext = m_threadContextLocator.get();

    if (threadContext != null) {
      return threadContext.getRunNumber();
    }

    return -1;
  }

  public Logger getLogger() {
    return m_logger;
  }

  public void sleep(long meanTime) throws GrinderException {
    m_sleeper.sleepNormal(meanTime);
  }

  public void sleep(long meanTime, long sigma) throws GrinderException {
    m_sleeper.sleepNormal(meanTime, sigma);
  }

  public int startWorkerThread() throws GrinderException {
    return m_threadStarter.startThread();
  }

  public FilenameFactory getFilenameFactory() {
    return m_filenameFactory;
  }

  public GrinderProperties getProperties() {
    return m_properties;
  }

  public Statistics getStatistics() {
    return m_scriptStatistics;
  }

  public SSLControl getSSLControl() {
    return m_sslControl;
  }

  public TestRegistry getTestRegistry() {
    return m_testRegistry;
  }
}
