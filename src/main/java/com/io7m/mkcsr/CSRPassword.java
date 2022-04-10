/*
 * Copyright © 2014 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import com.io7m.jnull.NullCheck;

import javax.swing.JPasswordField;

final class CSRPassword
{
  private final char[] password;

  CSRPassword(
    final char[] in_password,
    final char[] in_password_confirmed)
    throws ValidationProblem
  {
    NullCheck.notNull(in_password, "Password");
    NullCheck.notNull(in_password_confirmed, "Confirmation");

    if (in_password.length < 8) {
      throw new ValidationProblem(
        "Password must be at least eight characters");
    }

    if (in_password.length != in_password_confirmed.length) {
      throw new ValidationProblem(
        "Password fields do not match (different lengths)");
    }

    for (int index = 0; index < in_password.length; ++index) {
      if (in_password[index] != in_password_confirmed[index]) {
        throw new ValidationProblem("Password fields do not match");
      }
    }

    this.password = in_password;
  }

  static CSRPassword fromPasswordFields(
    final JPasswordField pass,
    final JPasswordField confirm)
    throws ValidationProblem
  {
    try {
      final CSRPassword p =
        new CSRPassword(pass.getPassword(), confirm.getPassword());
      TextFieldUtilities.fieldRestoreVisual(pass);
      TextFieldUtilities.fieldRestoreVisual(confirm);
      return p;
    } catch (final ValidationProblem e) {
      TextFieldUtilities.fieldSetErrorVisual(pass);
      TextFieldUtilities.fieldSetErrorVisual(confirm);
      throw e;
    }
  }

  public char[] getPassword()
  {
    return this.password;
  }
}
