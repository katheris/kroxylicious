/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.config.secret;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A reference to the file containing a plain text password in UTF-8 encoding.  If the password file
 * contains more than one line, only the characters of the first line are taken to be the password,
 * excluding the line ending.  Subsequent lines are ignored.
 *
 * @param filePath file containing the password,
 *
 * @deprecated use FilePassword instead
 *
 * TODO: Once https://github.com/FasterXML/jackson-databind/pull/4335 is released (2.17), replace FilePasswordFilePath
 * with a @JsonAlias("filePath") annotation on the FilePassword#passwordFile formal parameter.
 */
@Deprecated(since = "0.5.0", forRemoval = true)
@SuppressWarnings({ "java:S5738", "java:S1133" }) // java:S5738 warns of the use and need to remove deprecated classes.
public record FilePasswordFilePath(String filePath) implements PasswordProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePasswordFilePath.class);

    public FilePasswordFilePath {
        LOGGER.warn("'filePath' property is deprecated. Use property 'passwordFile' instead.");
        Objects.requireNonNull(filePath);
    }

    @Override
    public String getProvidedPassword() {
        return FilePassword.readPasswordFile(filePath);
    }

    @Override
    public String toString() {
        return "FilePasswordFilePath[" +
                "filePath=" + filePath + ']';
    }

}
