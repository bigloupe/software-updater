// Copyright (c) 2012 Chan Wai Shing
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
package updater.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A tool that redirect the input stream to output stream. This is not efficient
 * and just suitable for small amout of data flow.
 *
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class StreamRedirect {

  private static final Logger LOG = Logger.getLogger(StreamRedirect.class.getName());
  protected InputStream in;
  protected OutputStream out;
  protected Thread thread;

  public StreamRedirect(InputStream in, OutputStream out) {
    if (in == null) {
      throw new NullPointerException("argument 'in' cannot be null");
    }
    if (out == null) {
      throw new NullPointerException("argument 'out' cannot be null");
    }
    this.in = in;
    this.out = out;
  }

  /**
   * Start the redirect process. Once started and stopped, cannot be restarted
   * again.
   */
  public synchronized void start() {
    if (thread != null) {
      return;
    }

    Runnable redirectTask = new Runnable() {

      @Override
      public void run() {
        try {
          int read;
          while ((read = in.read()) != -1) {
            out.write(read);
          }
        } catch (IOException ex) {
        }
      }
    };

    thread = new Thread(redirectTask);
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * Stop the redirect process. Note that it depends on whether the input stream {@code in}
   * will reply on interrupt action.
   */
  public synchronized void stop() {
    if (thread == null) {
      return;
    }
    thread.interrupt();
  }
}
