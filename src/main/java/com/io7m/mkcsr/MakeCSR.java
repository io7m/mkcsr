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

import com.io7m.jlog.Log;
import com.io7m.jlog.LogPolicyProperties;
import com.io7m.jlog.LogUsableType;
import com.io7m.jnull.Nullable;
import com.io7m.jproperties.JPropertyException;
import com.io7m.junreachable.UnreachableCodeException;
import net.java.dev.designgridlayout.DesignGridLayout;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.util.Properties;

/**
 * The main program.
 */

public final class MakeCSR extends JPanel
{
  private static final long serialVersionUID;

  static {
    serialVersionUID = -8270459543637058532L;
  }

  private final JButton ok;
  private final JTextField outdir;
  private final JPasswordField password;
  private final JPasswordField password_confirm;
  private final JTextField common_name;
  private final StatusPanel status;

  private MakeCSR(
    final LogUsableType log,
    final JFrame window)
    throws IOException
  {
    final DesignGridLayout dg = new DesignGridLayout(this);

    this.common_name = new JTextField(16);

    this.password = new JPasswordField(16);
    this.password_confirm = new JPasswordField(16);
    this.status = new StatusPanel();

    this.outdir = new JTextField(16);
    this.outdir.setEditable(false);
    this.outdir.setToolTipText("No directory selected");

    final JButton outdir_select = new JButton("Select...");
    outdir_select.addActionListener(e -> {
      final JFileChooser chooser = new JFileChooser();
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      chooser.setMultiSelectionEnabled(false);
      final int r = chooser.showOpenDialog(window);
      switch (r) {
        case JFileChooser.APPROVE_OPTION: {
          MakeCSR.this.outdir.setText(chooser.getSelectedFile().toString());
          MakeCSR.this.outdir
            .setToolTipText(
              "Certificate requests and keys will be written to: "
                + MakeCSR.this.outdir.getText());
          break;
        }
        case JFileChooser.CANCEL_OPTION: {
          MakeCSR.this.outdir.setText("");
          MakeCSR.this.outdir.setToolTipText("No directory selected");
          break;
        }
        default: {

        }
      }
    });

    this.ok = new JButton("OK");
    this.ok.addActionListener(e -> {
      MakeCSR.this.ok.setEnabled(false);

      try {
        final CSRPassword pass =
          CSRPassword.fromPasswordFields(
            MakeCSR.this.password,
            MakeCSR.this.password_confirm);
        final CSRUserName name1 =
          CSRUserName.fromField(MakeCSR.this.common_name);
        final File file =
          new File(TextFieldUtilities
                     .getFieldNonEmptyStringOrError(MakeCSR.this.outdir));

        final CSRDetails d = new CSRDetails(name1, pass, file);
        MakeCSR.this.status.unsetError();

        final boolean key_exists = d.getPrivateKeyFile().exists();
        if (key_exists) {
          final Object[] options = new String[2];
          options[0] = "Cancel";
          options[1] = "Overwrite";

          final StringBuilder message = new StringBuilder(128);
          message.append("Private key ");
          message.append(d.getPrivateKeyFile());
          message.append(" already exists, overwrite?");

          final int r =
            JOptionPane.showOptionDialog(
              window,
              message.toString(),
              "Overwrite?",
              JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE,
              null,
              options,
              options[0]);
          if (r == 0) {
            MakeCSR.this.ok.setEnabled(true);
            return;
          }
        }

        final CSRProgressWindow progress = new CSRProgressWindow(log, d);
        progress.addWindowListener(new WindowAdapter()
        {
          @Override
          public void windowClosing(
            final @Nullable WindowEvent e)
          {
            MakeCSR.this.ok.setEnabled(true);
          }
        });
        progress.pack();
        progress.setVisible(true);

      } catch (final ValidationProblem x) {
        MakeCSR.this.status.setError(x);
      }
    });

    final JLabel version = new JLabel(Version.get());
    version.setForeground(Color.gray);

    dg.row().grid().add(new JLabel("Username")).add(this.common_name, 2);
    dg.row().grid().add(new JLabel("Password")).add(this.password, 2);
    dg
      .row()
      .grid()
      .add(new JLabel("Password (Confirm)"))
      .add(this.password_confirm, 2);
    dg
      .row()
      .grid()
      .add(new JLabel("Output directory"))
      .add(this.outdir)
      .add(outdir_select);
    dg.emptyRow();
    dg.row().grid().add(this.ok);
    dg.emptyRow();
    dg.row().left().add(this.status).fill();
    dg.emptyRow();
    dg.row().left().add(version);
  }

  /**
   * Main function.
   *
   * @param args Command line arguments.
   */

  public static void main(
    final String[] args)
  {
    try {
      final BouncyCastleProvider provider = new BouncyCastleProvider();
      Security.addProvider(provider);

      final Properties props = new Properties();
      props.setProperty("com.io7m.mkcsr.logs.main", "true");
      props.setProperty("com.io7m.mkcsr.mkcsr.level", "LOG_DEBUG");
      final LogUsableType log =
        Log.newLog(
          LogPolicyProperties.newPolicy(props, "com.io7m.mkcsr"),
          "main");

      SwingUtilities.invokeLater(() -> {
        try {
          final JFrame window = new JFrame("MakeCSR");
          final MakeCSR csr = new MakeCSR(log, window);
          window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
          window.setContentPane(csr);
          window.pack();
          window.setVisible(true);
        } catch (final IOException x) {
          ErrorBox.showError(log, "I/O error", x);
          System.exit(1);
        }
      });
    } catch (final JPropertyException e) {
      throw new UnreachableCodeException(e);
    }
  }

  private static final class StatusPanel extends JPanel
  {
    private static final long serialVersionUID;

    static {
      serialVersionUID = -3544988558138712701L;
    }

    private final JLabel text;
    private final JLabel error_icon;

    StatusPanel()
      throws IOException
    {
      this.error_icon = Icons.makeErrorIcon();
      this.text = new JLabel("Some informative error text");
      this.unsetError();

      this.add(this.error_icon);
      this.add(this.text);
    }

    void setInfo(
      final String in_text)
    {
      this.setVisible(true);
      this.text.setText(in_text);
      this.error_icon.setVisible(false);
    }

    void setError(
      final Throwable x)
    {
      this.setVisible(true);
      this.text.setText(x.getMessage());
      this.error_icon.setVisible(true);
    }

    void unsetError()
    {
      this.setVisible(false);
    }
  }
}
