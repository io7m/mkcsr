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

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

final class CSRDetails
{
  private final CSRUserName user_name;
  private final CSRPassword password;
  private final File directory;
  private final File private_key;
  private final File csr;
  private final File hash_file;
  private final MessageDigest digest;
  private @Nullable String hash_value;

  CSRDetails(
    final CSRUserName in_user_name,
    final CSRPassword in_password,
    final File output)
  {
    try {
      this.user_name = NullCheck.notNull(in_user_name, "User name");
      this.password = NullCheck.notNull(in_password, "Password");
      this.directory = NullCheck.notNull(output, "File");
      this.private_key = new File(output, in_user_name.toString() + ".key");
      this.csr = new File(output, in_user_name.toString() + ".csr");
      this.hash_file = new File(output, in_user_name.toString() + ".sha256");
      this.digest = MessageDigest.getInstance("SHA256");
      this.hash_value = null;
    } catch (final NoSuchAlgorithmException e) {
      throw new UnreachableCodeException(e);
    }
  }

  static KeyPair generateKeyPair()
    throws NoSuchAlgorithmException,
    NoSuchProviderException
  {
    final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
    kpg.initialize(4096);
    return kpg.generateKeyPair();
  }

  PKCS10CertificationRequest generateCSR(
    final KeyPair keypair)
    throws IOException,
    OperatorCreationException
  {
    final StringBuilder b = new StringBuilder(128);
    b.append("CN=");
    b.append(this.user_name.toString());
    b.append(", ");
    b.append("C=XA");

    final AsymmetricKeyParameter private_key_param =
      PrivateKeyFactory.createKey(keypair.getPrivate().getEncoded());
    final AlgorithmIdentifier signature_algorithm =
      new DefaultSignatureAlgorithmIdentifierFinder().find("SHA1WITHRSA");
    final AlgorithmIdentifier digest_algorithm =
      new DefaultDigestAlgorithmIdentifierFinder().find("SHA-1");
    final ContentSigner signer =
      new BcRSAContentSignerBuilder(signature_algorithm, digest_algorithm)
        .build(private_key_param);

    final PKCS10CertificationRequestBuilder csr_builder =
      new JcaPKCS10CertificationRequestBuilder(
        new X500Name(b.toString()),
        keypair.getPublic());
    final ExtensionsGenerator extensions_generator =
      new ExtensionsGenerator();
    extensions_generator.addExtension(
      X509Extension.basicConstraints,
      true,
      new BasicConstraints(true));
    extensions_generator.addExtension(
      X509Extension.keyUsage,
      true,
      new KeyUsage(KeyUsage.dataEncipherment));
    csr_builder.addAttribute(
      PKCSObjectIdentifiers.pkcs_9_at_extensionRequest,
      extensions_generator.generate());
    return csr_builder.build(signer);
  }

  public String getHashValue()
  {
    final String hv = this.hash_value;
    if (hv == null) {
      throw new IllegalStateException("Hash value not yet calculated");
    }
    return hv;
  }

  public File getHashFile()
  {
    return this.hash_file;
  }

  public File getCSRFile()
  {
    return this.csr;
  }

  public File getDirectory()
  {
    return this.directory;
  }

  public CSRPassword getPassword()
  {
    return this.password;
  }

  public File getPrivateKeyFile()
  {
    return this.private_key;
  }

  public CSRUserName getUserName()
  {
    return this.user_name;
  }

  void writeCSR(
    final PKCS10CertificationRequest csr_actual)
    throws IOException
  {
    try (final JcaPEMWriter writer = new JcaPEMWriter(new FileWriter(this.csr))) {
      writer.writeObject(csr_actual);
      writer.flush();
    }
  }

  void writeCSRHash()
    throws IOException
  {

    try (FileInputStream stream = new FileInputStream(this.csr)) {
      this.digest.reset();

      final byte[] buffer = new byte[8192];
      while (true) {
        final int r = stream.read(buffer);
        if (r == -1) {
          break;
        }
        this.digest.update(buffer, 0, r);
      }

      final byte[] r = this.digest.digest();
      final StringBuilder s = new StringBuilder();

      for (int index = 0; index < r.length; ++index) {
        s.append(String.format("%02x", Byte.valueOf(r[index])));
      }

      this.hash_value = s.toString();
      try (final PrintWriter out = new PrintWriter(new FileWriter(this.hash_file))) {
        out.println(this.hash_value);
        out.flush();
      }
    }
  }

  void writePrivateKey(
    final KeyPair keypair)
    throws OperatorCreationException,
    IOException
  {
    final JceOpenSSLPKCS8EncryptorBuilder builder =
      new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.PBE_SHA1_3DES);
    builder.setRandom(new SecureRandom());
    builder.setPasssword(this.password.getPassword());
    builder.setIterationCount(100000);
    final OutputEncryptor oe = builder.build();
    final JcaPKCS8Generator gen =
      new JcaPKCS8Generator(keypair.getPrivate(), oe);
    final PemObject pem = gen.generate();

    try (JcaPEMWriter writer = new JcaPEMWriter(new FileWriter(this.private_key))) {
      writer.writeObject(pem);
      writer.flush();
    }
  }
}
