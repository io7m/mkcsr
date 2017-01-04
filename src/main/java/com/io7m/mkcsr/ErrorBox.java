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

import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;

final class ErrorBox
{
  private ErrorBox()
  {
    throw new UnreachableCodeException();
  }

  public static void showError(
    final Logger log,
    final String title,
    final Throwable e)
  {
    log.error(title + ": " + e.getMessage());

    SwingUtilities.invokeLater(() -> showErrorWithException(title, e));
  }

  private static void showErrorBox(
    final String title,
    final JTextArea backtrace)
  {
    final JScrollPane pane = new JScrollPane(backtrace);
    pane.setPreferredSize(new Dimension(600, 320));

    final JLabel label = new JLabel(title);
    label.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

    final BorderLayout layout = new BorderLayout();
    final JPanel panel = new JPanel(layout);
    panel.add(label, BorderLayout.NORTH);
    panel.add(pane, BorderLayout.SOUTH);

    JOptionPane.showMessageDialog(
      null,
      panel,
      title,
      JOptionPane.ERROR_MESSAGE);
  }

  private static void showErrorWithException(
    final String title,
    final Throwable e)
  {
    try (final StringWriter writer = new StringWriter()) {
      writer.append(e.getMessage());
      writer.append(System.lineSeparator());
      writer.append(System.lineSeparator());

      e.printStackTrace(new PrintWriter(writer));
      e.printStackTrace();

      final JTextArea text = new JTextArea();
      text.setEditable(false);
      text.setText(writer.toString());

      showErrorBox(title, text);
    } catch (final IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public static void showErrorWithoutException(
    final Logger log,
    final String title,
    final String message)
  {
    log.error(title + ": " + message);

    SwingUtilities.invokeLater(() -> {
      final JTextArea text = new JTextArea();
      text.setEditable(false);
      text.setText(message);

      showErrorBox(title, text);
    });
  }
}
