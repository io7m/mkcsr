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

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.border.Border;
import java.awt.Color;

final class TextFieldUtilities
{
  private static final Border DEFAULT_FIELD_BORDER;
  private static final Color DEFAULT_FIELD_BACKGROUND;

  static {
    final JTextField f = new JTextField();
    DEFAULT_FIELD_BORDER = f.getBorder();
    DEFAULT_FIELD_BACKGROUND = f.getBackground();
  }

  private TextFieldUtilities()
  {
    throw new UnreachableCodeException();
  }

  static void fieldRestoreVisual(
    final JTextField field)
  {
    field.setBorder(DEFAULT_FIELD_BORDER);
    field.setBackground(DEFAULT_FIELD_BACKGROUND);
  }

  static void fieldSetErrorVisual(
    final JTextField field)
  {
    field.setBorder(BorderFactory.createLineBorder(Color.RED));
    field.setBackground(Color.PINK);
  }

  static String getFieldNonEmptyStringOrError(
    final JTextField field)
    throws ValidationProblem
  {
    final String s = field.getText();
    if (s.isEmpty()) {
      fieldSetErrorVisual(field);
      throw new ValidationProblem("Field must not be empty");
    }
    fieldRestoreVisual(field);
    return s;
  }
}
