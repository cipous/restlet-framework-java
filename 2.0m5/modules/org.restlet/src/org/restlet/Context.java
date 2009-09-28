/**
 * Copyright 2005-2009 Noelios Technologies.
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

package org.restlet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.util.Series;

/**
 * Contextual data and services provided to a Restlet. The context is the means
 * by which a Restlet may access the software environment within the framework.
 * It is typically provided by the immediate parent Restlet (Application is the
 * most common case).<br>
 * <br>
 * Concurrency note: attributes and parameters of a context are stored in
 * concurrent collections that guarantee thread safe access and modification. If
 * several threads concurrently access objects and modify these collections,
 * they should synchronize on the lock of the Context instance.
 * 
 * @author Jerome Louvel
 */
public class Context {

    // [ifndef gwt] member
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<Context>();

    /**
     * Returns the context associated to the current Restlet. The context can be
     * the one of a Component, an Application, a Filter or any other Restlet
     * subclass.
     * 
     * Warning: this method should only be used under duress. You should by
     * default prefer obtaining the current context using methods such as
     * {@link org.restlet.Restlet#getContext()} or
     * {@link org.restlet.resource.Resource#getContext()}.
     * 
     * This variable is stored internally as a thread local variable and updated
     * each time a request is handled by a Restlet via the
     * {@link Restlet#handle(org.restlet.Request, org.restlet.Response)}
     * method.
     * 
     * @return The current context.
     */
    public static Context getCurrent() {
        // [ifndef gwt] line
        return CURRENT.get();
        // [ifdef gwt] line uncomment
        // return new Context();
    }

    /**
     * Returns the current context's logger.
     * 
     * @return The current context's logger.
     */
    public static Logger getCurrentLogger() {
        // [ifndef gwt] instruction
        return (Context.getCurrent() != null) ? Context.getCurrent()
                .getLogger() : Logger.getLogger(Context.class
                .getCanonicalName());

        // [ifdef gwt] instruction uncomment
        // return Logger.getLogger(Context.class.getName());
    }

    // [ifndef gwt] method
    /**
     * Sets the context to associated with the current thread.
     * 
     * @param context
     *            The thread's context.
     */
    public static void setCurrent(Context context) {
        CURRENT.set(context);
    }

    /** The client dispatcher. */
    private volatile Client clientDispatcher;

    // [ifndef gwt] member
    /** The server dispatcher. */
    private volatile Client serverDispatcher;

    /** The modifiable attributes map. */
    private final ConcurrentMap<String, Object> attributes;

    /** The logger instance to use. */
    private volatile Logger logger;

    /** The modifiable series of parameters. */
    private final Series<Parameter> parameters;

    // [ifndef gwt] member
    /**
     * The enroler that can add the user roles based on Restlet default
     * authorization model.
     */
    private volatile org.restlet.security.Enroler enroler;

    // [ifndef gwt] member
    /**
     * The verifier that can check the validity of user/secret couples based on
     * Restlet default authorization model.
     */
    private volatile org.restlet.security.Verifier verifier;

    /**
     * Constructor. Writes log messages to "org.restlet".
     */
    public Context() {
        this("org.restlet");
    }

    /**
     * Constructor.
     * 
     * @param logger
     *            The logger instance of use.
     */
    public Context(Logger logger) {
        this.attributes = new ConcurrentHashMap<String, Object>();
        this.logger = logger;
        this.parameters = new Form(new CopyOnWriteArrayList<Parameter>());
        this.clientDispatcher = null;

        // [ifndef gwt]
        this.enroler = null;
        this.serverDispatcher = null;
        this.verifier = null;
        // [enddef]
    }

    /**
     * Constructor.
     * 
     * @param loggerName
     *            The name of the logger to use.
     */
    public Context(String loggerName) {
        this(Logger.getLogger(loggerName));
    }

    /**
     * Creates a protected child context. This is especially useful for new
     * application attached to their parent component, to ensure their isolation
     * from the other applications. By default it just creates a new context
     * instance.
     * 
     * @return The child context.
     */
    public Context createChildContext() {
        return new Context();
    }

    /**
     * Returns a modifiable attributes map that can be used by developers to
     * save information relative to the context. This is a convenient means to
     * provide common objects to all the Restlets and Resources composing an
     * Application.<br>
     * <br>
     * 
     * In addition, this map is a shared space between the developer and the
     * Restlet implementation. For this purpose, all attribute names starting
     * with "org.restlet" are reserved. Currently the following attributes are
     * used:
     * <table>
     * <tr>
     * <th>Attribute name</th>
     * <th>Class name</th>
     * <th>Description</th>
     * </tr>
     * <tr>
     * <td>org.restlet.application</td>
     * <td>org.restlet.Application</td>
     * <td>The parent application providing this context, if any.</td>
     * </tr>
     * </table>
     * </td>
     * 
     * @return The modifiable attributes map.
     */
    public ConcurrentMap<String, Object> getAttributes() {
        return this.attributes;
    }

    /**
     * Returns a request dispatcher to available client connectors. When you ask
     * the dispatcher to handle a request, it will automatically select the
     * appropriate client connector for your request, based on the
     * request.protocol property or on the resource URI's scheme. This call is
     * blocking and will return an updated response object.
     * 
     * @return A request dispatcher to available client connectors.
     */
    public Client getClientDispatcher() {
        return this.clientDispatcher;
    }

    // [ifndef gwt] method
    /**
     * Returns an enroler that can add the user roles based on authenticated
     * user principals.
     * 
     * @return An enroler.
     */
    public org.restlet.security.Enroler getEnroler() {
        return enroler;
    }

    /**
     * Returns the logger.
     * 
     * @return The logger.
     */
    public Logger getLogger() {
        return this.logger;
    }

    /**
     * Returns the modifiable series of parameters. A parameter is a pair
     * composed of a name and a value and is typically used for configuration
     * purpose, like Java properties. Note that multiple parameters with the
     * same name can be declared and accessed.
     * 
     * @return The modifiable series of parameters.
     */
    public Series<Parameter> getParameters() {
        return this.parameters;
    }

    // [ifndef gwt] method
    /**
     * Returns a request dispatcher to component's virtual hosts. This is mostly
     * useful for application that want to optimize calls to other applications
     * hosted in the same component or to the application itself.<br>
     * <br>
     * The processing is the same as what would have been done if the request
     * came from one of the component's server connectors. It first must match
     * one of the registered virtual hosts. Then it can be routed to one of the
     * attached Restlets, typically an Application.
     * 
     * @return A request dispatcher to the server connectors' router.
     */
    public Client getServerDispatcher() {
        return this.serverDispatcher;
    }

    // [ifndef gwt] method
    /**
     * Returns a verifier that can check the validity of the credentials
     * associated to a request.
     * 
     * @return A verifier.
     */
    public org.restlet.security.Verifier getVerifier() {
        return this.verifier;
    }

    /**
     * Sets the modifiable map of attributes.
     * 
     * @param attributes
     *            The modifiable map of attributes.
     */
    public synchronized void setAttributes(Map<String, Object> attributes) {
        this.attributes.clear();
        this.attributes.putAll(attributes);
    }

    /**
     * Sets the client dispatcher.
     * 
     * @param clientDispatcher
     *            The new client dispatcher.
     */
    public void setClientDispatcher(Client clientDispatcher) {
        this.clientDispatcher = clientDispatcher;
    }

    // [ifndef gwt] method
    /**
     * Sets an enroler that can add the user roles based on authenticated user
     * principals.
     * 
     * @param enroler
     *            An enroler.
     */
    public void setEnroler(org.restlet.security.Enroler enroler) {
        this.enroler = enroler;
    }

    /**
     * Sets the logger.
     * 
     * @param logger
     *            The logger.
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Sets the logger.
     * 
     * @param loggerName
     *            The name of the logger to use.
     */
    public void setLogger(String loggerName) {
        setLogger(Logger.getLogger(loggerName));
    }

    /**
     * Sets the modifiable series of parameters.
     * 
     * @param parameters
     *            The modifiable series of parameters.
     */
    public synchronized void setParameters(Series<Parameter> parameters) {
        this.parameters.clear();

        if (parameters != null) {
            this.parameters.addAll(parameters);
        }
    }

    // [ifndef gwt] method
    /**
     * Sets the server dispatcher.
     * 
     * @param serverDispatcher
     *            The new server dispatcher.
     */
    public void setServerDispatcher(Client serverDispatcher) {
        this.serverDispatcher = serverDispatcher;
    }

    // [ifndef gwt] method
    /**
     * Sets a local verifier that can check the validity of user/secret couples
     * based on Restlet default authorization model.
     * 
     * @param verifier
     *            A local verifier.
     */
    public void setVerifier(org.restlet.security.Verifier verifier) {
        this.verifier = verifier;
    }

}
