// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2008 Philip Aston
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

package net.grinder.console;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import net.grinder.common.GrinderException;
import net.grinder.common.Logger;
import net.grinder.communication.Message;
import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.communication.MessageDispatchRegistry.AbstractHandler;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.Resources;
import net.grinder.console.communication.ConsoleCommunication;
import net.grinder.console.communication.ConsoleCommunicationImplementation;
import net.grinder.console.communication.DistributionControlImplementation;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.communication.ProcessControlImplementation;
import net.grinder.console.communication.ProcessStatus;
import net.grinder.console.communication.server.DispatchClientCommands;
import net.grinder.console.distribution.FileDistributionImplementation;
import net.grinder.console.messages.RegisterExpressionViewMessage;
import net.grinder.console.messages.RegisterTestsMessage;
import net.grinder.console.messages.ReportStatisticsMessage;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.Model;
import net.grinder.console.model.ModelImplementation;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.util.Directory;

import org.picocontainer.MutablePicoContainer;
import org.picocontainer.Parameter;
import org.picocontainer.defaults.ComponentParameter;
import org.picocontainer.defaults.ConstantParameter;
import org.picocontainer.defaults.DefaultPicoContainer;


/**
 * This is the entry point of The Grinder console.
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 **/
public final class ConsoleFoundation {

  private final MutablePicoContainer m_container;

  /**
   * Constructor. Locates the console properties in the user's home directory.
   *
   * @param resources Console resources
   * @param logger Logger.
   *
   * @exception GrinderException If an error occurs.
   */
  public ConsoleFoundation(Resources resources, Logger logger)
    throws GrinderException {

    this(resources,
         logger,
         new Timer(true),
         new ConsoleProperties(
            resources,
            // Some platforms do not have user home directories, fall back
            // to java.home.
            new File(System.getProperty("user.home",
                       System.getProperty("java.home")),
                     ".grinder_console")));
  }

  /**
   * Constructor. Allows properties to be specified.
   *
   * @param resources Console resources
   * @param logger Logger.
   * @param timer A timer.
   * @param properties The properties.
   *
   * @exception GrinderException If an error occurs.
   */
  public ConsoleFoundation(Resources resources,
                           Logger logger,
                           Timer timer,
                           ConsoleProperties properties)
    throws GrinderException {

    m_container = new DefaultPicoContainer();
    m_container.registerComponentInstance(logger);
    m_container.registerComponentInstance(resources);
    m_container.registerComponentInstance(properties);
    m_container.registerComponentInstance(timer);
    m_container.registerComponentInstance(
      StatisticsServicesImplementation.getInstance());

    m_container.registerComponentImplementation(ModelImplementation.class);

    m_container.registerComponentImplementation(
      ConsoleCommunicationImplementation.class);

    m_container.registerComponentImplementation(
      DistributionControlImplementation.class);

    m_container.registerComponentImplementation(
      FileDistributionImplementation.class,
      FileDistributionImplementation.class,
      new Parameter[] {
        new ComponentParameter(DistributionControlImplementation.class),
        new ConstantParameter(properties.getDistributionDirectory()),
        new ConstantParameter(properties.getDistributionFileFilterPattern()),
      });

    m_container.registerComponentImplementation(DispatchClientCommands.class);

    m_container.registerComponentImplementation(WireFileDistribution.class);

    m_container.registerComponentImplementation(WireMessageDispatch.class);

    m_container.registerComponentImplementation(
      ProcessControlImplementation.class);
  }

  /**
   * Factory method to create a console user interface implementation.
   * PicoContainer is used to satisfy the requirements of the implementation's
   * constructor.
   *
   * @param uiClass
   *            The implementation class - must implement
   *            {@link ConsoleFoundation#UI}.
   * @return An instance of the user interface class.
   */
  public UI createUI(Class uiClass) {
    m_container.registerComponentImplementation(uiClass);

    final UI ui = (UI) m_container.getComponentInstanceOfType(uiClass);

    final ConsoleCommunication communication =
      (ConsoleCommunication)m_container.getComponentInstanceOfType(
        ConsoleCommunication.class);

    communication.setErrorHandler(ui.getErrorHandler());

    return ui;
  }

  /**
   * Shut down the console.
   *
   */
  public void shutdown() {
    final ConsoleCommunication communication =
      (ConsoleCommunication)m_container.getComponentInstanceOfType(
        ConsoleCommunication.class);

    communication.shutdown();
  }

  /**
   * Console message event loop. Dispatches communication messages
   * appropriately. Blocks until we are {@link #shutdown()}.
   */
  public void run() {
    m_container.start();

    final ConsoleCommunication communication =
      (ConsoleCommunication)m_container.getComponentInstanceOfType(
        ConsoleCommunication.class);

    // Need to request components, or they won't be instantiated.
    m_container.getComponentInstanceOfType(WireMessageDispatch.class);
    m_container.getComponentInstanceOfType(WireFileDistribution.class);

    while (communication.processOneMessage()) {
      // Process until communication is shut down.
    }
  }

  /**
   * Contract for user interfaces.
   *
   * @author Philip Aston
   * @version $Revision$
   */
  public interface UI {
    /**
     * Return an error handler to which errors should be reported.
     *
     * @return The error handler.
     */
    ErrorHandler getErrorHandler();
  }

  /**
   * Factory that wires up the FileDistribution. As far as I can see, Pico
   * forces us to use a constructor. Would be nicer if we could say
   * <pre>
   *    container.call(MyFactory.class, "myMethod");
   * </pre>
   *
   * <p>Must be public for PicoContainer.</p>
   */
  public static class WireFileDistribution {

    /**
     * Constructor for WireFileDistribution.
     *
     * @param fileDistribution A file distribution.
     * @param properties The console properties.
     * @param timer A timer.
     * @param processControl A process control
     */
    public WireFileDistribution(
      final FileDistributionImplementation fileDistribution,
      ConsoleProperties properties,
      Timer timer,
      ProcessControl processControl) {

      timer.schedule(new TimerTask() {
          public void run() {
            fileDistribution.scanDistributionFiles();
          }
        },
        properties.getScanDistributionFilesPeriod(),
        properties.getScanDistributionFilesPeriod());


      processControl.addProcessStatusListener(
        new ProcessStatus.Listener() {
          public void update(ProcessStatus.ProcessReports[] processStatuses,
                             boolean newAgent) {
            if (newAgent) {
              fileDistribution.getAgentCacheState().setOutOfDate();
            }
          }
        });

      properties.addPropertyChangeListener(
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent e) {
            final String propertyName = e.getPropertyName();

            if (propertyName.equals(
              ConsoleProperties.DISTRIBUTION_DIRECTORY_PROPERTY)) {
              fileDistribution.setDirectory((Directory)e.getNewValue());
            }
            else if (propertyName.equals(
              ConsoleProperties.DISTRIBUTION_FILE_FILTER_EXPRESSION_PROPERTY)) {
              fileDistribution.setFileFilterPattern((Pattern) e.getNewValue());
            }
          }
        });
    }
  }

  /**
   * Factory that wires up the message dispatch.
   *
   * <p>Must be public for PicoContainer.</p>
   *
   * @see WireFileDistribution
   */
  public static class WireMessageDispatch {

    /**
     * Constructor for WireFileDistribution.
     *
     * @param communication Console communication.
     * @param model Console model
     * @param dispatchClientCommands Client command dispatcher.
     */
    public WireMessageDispatch(ConsoleCommunication communication,
                               final Model model,
                               DispatchClientCommands dispatchClientCommands) {

      final MessageDispatchRegistry messageDispatchRegistry =
        communication.getMessageDispatchRegistry();

      messageDispatchRegistry.set(
        RegisterTestsMessage.class,
        new AbstractHandler() {
          public void send(Message message) {
            model.registerTests(((RegisterTestsMessage)message).getTests());
          }
        });

      messageDispatchRegistry.set(
        ReportStatisticsMessage.class,
        new AbstractHandler() {
          public void send(Message message) {
            model.addTestReport(
              ((ReportStatisticsMessage)message).getStatisticsDelta());
          }
        });

      messageDispatchRegistry.set(
        RegisterExpressionViewMessage.class,
        new AbstractHandler() {
          public void send(Message message) {
            model.registerStatisticExpression(
              ((RegisterExpressionViewMessage)message).getExpressionView());
          }
        });

      dispatchClientCommands.registerMessageHandlers(messageDispatchRegistry);
    }
  }
}
