/**
 * Copyright 2005-2010 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * 
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */

package org.restlet.engine.http.header;

import java.util.List;

import org.restlet.data.Expectation;

/**
 * Expectation header writer.
 * 
 * @author Jerome Louvel
 */
public class ExpectationWriter extends HeaderWriter {

    /**
     * Formats a list of expectations with a comma separator.
     * 
     * @param expectations
     *            The list of expectations.
     * @return The formatted list of expectations.
     * @throws IllegalArgumentException
     */
    public static String write(List<Expectation> expectations)
            throws IllegalArgumentException {
        return new ExpectationWriter().append(expectations).toString();
    }

    /**
     * Formats a cache directive.
     * 
     * @param expectation
     *            The directive to format.
     */
    public ExpectationWriter append(Expectation expectation) {
        append(expectation.getName());

        if ((expectation.getValue() != null)
                && (expectation.getValue().length() > 0)) {
            // if (directive.isDigit()) {
            // destination.append("=").append(directive.getValue());
            // } else {
            // destination.append("=\"").append(directive.getValue()).append(
            // '\"');
            // }
        }

        return this;
    }

    /**
     * Formats a list of expectations with a comma separator.
     * 
     * @param expectations
     *            The list of expectations.
     * @return The formatted list of expectations.
     * @throws IllegalArgumentException
     */
    public ExpectationWriter append(List<Expectation> expectations)
            throws IllegalArgumentException {
        Expectation expectation;

        for (int i = 0; i < expectations.size(); i++) {
            if (i > 0) {
                append(", ");
            }

            expectation = expectations.get(i);
            append(expectation);
        }

        return this;
    }
}
