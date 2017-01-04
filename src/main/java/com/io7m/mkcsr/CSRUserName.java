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

import javax.swing.JTextField;

final class CSRUserName
{
  private final String actual;

  CSRUserName(
    final String name)
    throws ValidationProblem
  {
    if (name.length() < 3) {
      throw new ValidationProblem(
        "Username must be at least three characters long");
    }
    if (!name.matches("[A-Za-z0-9_]+")) {
      throw new ValidationProblem(
        "Username can only contain letters, digits, and underscores");
    }
    this.actual = name;
  }

  static CSRUserName fromField(
    final JTextField field)
    throws ValidationProblem
  {
    try {
      final CSRUserName un = new CSRUserName(field.getText());
      TextFieldUtilities.fieldRestoreVisual(field);
      return un;
    } catch (final ValidationProblem e) {
      TextFieldUtilities.fieldSetErrorVisual(field);
      throw e;
    }
  }

  @Override
  public String toString()
  {
    return this.actual;
  }
}
