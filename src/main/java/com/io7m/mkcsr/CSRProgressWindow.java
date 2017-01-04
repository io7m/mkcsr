/*
 * Copyright © 2014 <code@io7m.com> http://io7m.com
 * 
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.mkcsr;

import com.io7m.jlog.LogUsableType;
import com.io7m.jnull.Nullable;
import net.java.dev.designgridlayout.DesignGridLayout;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

final class CSRProgressWindow extends JFrame
{
  private static final long serialVersionUID;

  static {
    serialVersionUID = -5725875455674952427L;
  }

  private final JTextArea area;
  private final AtomicBoolean done;

  CSRProgressWindow(
    final LogUsableType log,
    final CSRDetails d)
  {
    super("Progress");
    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    this.addWindowListener(new WindowAdapter()
    {
      @SuppressWarnings("synthetic-access")
      @Override
      public void
      windowClosing(
        final @Nullable WindowEvent e)
      {
        boolean close = false;

        if (!CSRProgressWindow.this.done.get()) {
          final Object[] options = new String[2];
          options[0] = "Continue";
          options[1] = "Stop";

          final int r =
            JOptionPane.showOptionDialog(
              CSRProgressWindow.this,
              "Key and CSR generation is still in progress. Really stop?",
              "Stop?",
              JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE,
              null,
              options,
              options[0]);
          if (r == 1) {
            close = true;
          }
        } else {
          close = true;
        }

        if (close) {
          CSRProgressWindow.this.dispose();
        }
      }
    });

    this.done = new AtomicBoolean();
    this.area = new JTextArea();
    this.area.setPreferredSize(new Dimension(640, 320));
    this.area.setEditable(false);
    final JScrollPane scroll = new JScrollPane(this.area);
    scroll
      .setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

    final DesignGridLayout dg = new DesignGridLayout(this.getContentPane());
    dg.row().grid().add(scroll);

    final SwingWorker<Void, String> worker = new SwingWorker<Void, String>()
    {
      @Override
      protected
      @Nullable
      Void doInBackground()
        throws Exception
      {
        try {
          this
            .publish(
              "Generating private key (can take ~30 seconds on a reasonably fast machine)...");
          final KeyPair kp = CSRDetails.generateKeyPair();

          this.publish("Encrypting and saving private key to "
                         + d.getPrivateKeyFile()
                         + "...");
          d.writePrivateKey(kp);

          this.publish("Generating certificate signing request...");
          final PKCS10CertificationRequest csr = d.generateCSR(kp);

          this.publish("Writing certificate signing request to "
                         + d.getCSRFile()
                         + "...");
          d.writeCSR(csr);

          this.publish("Hashing CSR and saving hash to "
                         + d.getHashFile()
                         + "...");
          d.writeCSRHash();

          this.publish("The hash value of your CSR is "
                         + d.getHashValue()
                         + ".");

          this.publish("Completed successfully.");
          CSRProgressWindow.this.done.set(true);
          return null;
        } catch (final Exception x) {
          x.fillInStackTrace();
          final StringBuilder b = new StringBuilder(128);
          b.append("Fatal: ");

          while (true) {
            b.append(x.getMessage());
            b.append(System.lineSeparator());

            for (final StackTraceElement e : x.getStackTrace()) {
              b.append(" at ");
              b.append(e.getClassName());
              b.append(" ");
              b.append(e.getFileName());
              b.append(":");
              b.append(e.getLineNumber());
              b.append(System.lineSeparator());
            }
            if (x.getCause() != null) {
              b.append("Caused by:");
              b.append(System.lineSeparator());
            } else {
              break;
            }
          }

          this.publish(b.toString());
          CSRProgressWindow.this.done.set(true);
          x.printStackTrace();
          throw x;
        }
      }

      @Override
      protected void process(
        final @Nullable List<String> chunks)
      {
        assert chunks != null;

        for (final String c : chunks) {
          CSRProgressWindow.this.area.append(c + System.lineSeparator());
          log.info(c);
        }
      }
    };

    worker.execute();
  }
}
