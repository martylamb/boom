# Boom - a collection of helpers for building simple web applications on the Spark framework

Boom is a simple convention-over-configuration library that wraps around [Spark](http://sparkjava.com) to allow simple web applications to be built extremely quickly.  Its primary goal is to match my own development style and reduce boilerplate.  Hopefully you'll find it useful, too.

```java
import static com.martiansoftware.boom.Boom.*;
```
Use com.martiansoftware.boom.Boom in place of spark.Spark for additional functionality.  Spark is used behind the scenes by Boom.  You can still call Spark directly from the same application if you like.



## BoomRoute

When registering Routes (e.g., Spark's get() and post() methods), you can alternately specify a BoomRoute, which takes no arguments and returns an Object.  Spark's Request and Response objects for the current Thread are made available by Boom's request() and response() methods, so your BoomRoute implementations can do this (assuming static method App.doThing()):

```java
get("/thing", App::doThing);
```

instead of this:
```java
get("/thing", (req,rsp) -> App.doThing(req,rsp));
```

## BoomResponse

Routes (and BoomRoutes) can continue return any type of Object for Spark to render to the client.  But if you return a BoomResponse then a number of things are taken care of for you automatically.

The BoomResponse() constructor can take a String, InputStream, File, or URL as an argument.  In each case you will get what you expect; the InputStream (or String, File, or URL contents, etc.) will be copied to the client.  If a File or URL has a file extension, Boom will set the appropriate MIME type as well.

Type helpers and a fluent interface allow a single call to performa multiple tasks like:
```java
private static Object doThing() {
	return new BoomResponse("{'thing':'done'}").json();
}
```

Boom provides some static methods for easier construction and initialization of BoomResponses as well:
```java
private static Object getThing() {
	// json-ification is automatic
	// mime type set to "application/json"
	return json(someObject);
}
```

```java
private static Object getFile() {
	// mime type set to "application/octet-stream"
	return binary(myFile);
}
```

```java
private static Object getXml() {
	// mime type set to "text/xml"
	return xml(something.getXmlInputStream());
}
```

```java
private static Object getText() {
	// mime type set to "text/plain"
	return text("just plain old text here...");
}
```

## Debug Mode

If the environment variable or system property `BOOM_DEBUG` is "1", then certain behaviors are modified to support development (described here and there below).

## Static Content

Static content is automatically configured to load from the classpath under /static-content.  For Maven projects, just put static content files into `src/main/resources/static-content` and it's all set up for you.  When your jar is bundled and delivered, static content will automatically be packaged and included by Maven.

**If running in Debug Mode**, then static content is instead automatically configured to load from the filesystem under `src/main/resources/static-content` instead of from your classpath.  This allows reloading of content from the filesystem during development without restarting your application.

## Templates

You can use Spark's built-in template functionality (or whatever else you'd like to use), but Boom provides helpers for use with [DumbTemplates](https://github.com/martylamb/dumbtemplates).  Template can be obtained from Boom via `template(templateName)`.

Behavior is similar to that for static content: templates are automatically configured to load from the classpath under `/templates`.  For Maven projects, just put them into `src/main/resources/templates` and it's all set up for you.  When your jar is bundled and delivered, templates will be automatically packaged and included by Maven.

**If running in Debug Mode**, then templates are instead automatically configured to load from the filesystem under `src/main/resources/templates` instead of from your classpath.  This allows reloading of templates from the filesystem during development without restarting your application.

## Resource Bundles

Boom provides a helper method for loading ResourceBundles.  If you store your bundles in the classpath under `/bundles`, then Boom's `r(bundlename)` will retrieve it for you.  For Maven projects, put them into `src/main/resources/bundles`.

A future update will store end user locale information in the session and use that to load the appropriate bundle automatically.  Of course, you'll be able to manually set the user's locale as well.

## Custom Error/Status Pages

To customize the html returned on exceptions or halts, use [DumbTemplates](https://github.com/martylamb/dumbtemplates) in your classpath under `/templates/boom/status/CODE.html`, where CODE is the status code for which you are customizing the output.  For example, for a custom Error 503 page, use `/templates/boom/status/503.html`.  If no template is found, `/templates/boom/status/default.html` is then tried, so you can provide a general override if you like.  Boom will place "status" and "body" values in the template context.

## Authentication

Boom provides form-based authentication (you're using SSL/TLS, right?)  Setting it up is easy but it does require a few steps.

  * Create an `Authenticator`.  This is a functional interface in `com.martiansoftware.boom.auth` that takes a username and passphrase and returns an authenticated `User` object (see `com.martiansoftware.boom.auth.User`) if login is successful, and returns null otherwise:
```java
public interface Authenticator {    
    public User authenticate(String username, String passphrase);
}
```
  * Create a `FormLoginFilter`.  This is a class in `com.martiansoftware.boom.auth` that filters requests and presents a login page when required.  The default login page templates are in `boom-default-templates.FormLoginFilter` and can be customized by placing your own `FormLoginFilter/login.html` and `FormLoginFilter/loggedout.html` templates in your own template directory as described above.  Take a look at the default login.html form to see the appropriate query parameters and POST destination.  `FormLoginFilter`'s constructor takes a single argument: the `Authenticator` you created in the previous step.
  * Exempt any paths that DO NOT require authentication in the `FormLoginFilter` via its exempt() method.  This method returns the modified `FormLoginFilter` so it may be chained as follows:
```java
myFormLoginFilter.exempt("/favicon.ico").exempt("/styles.css").exempt("/images/logo.png");
```
  * Tell Boom to use your `FormLoginFilter` via `Boom.login()`.

### Authentication Example

Here's a simple example that creates a (very dumb) `Authenticator` and sets up authentication:
```java
Authenticator a = (String username, String passphrase) -> {
    if("testuser".equalsIgnoreCase(username) && "letmein".equals(passphrase)) {
        return new User("testuser");
    } else {
        return null;
    }
};
        
login(new FormLoginFilter(a).exempt("/favicon.ico"));
```
Boom will automatically exempt the `/login` and `/logout` paths used by `FormLoginFilter`.




## TODO

A bunch of things remain planned:

  * Automatic CSRF protection (already done in another project, needs to be extracted and cleaned up)
  * i18n
  * maybe provide separate jars bundling existing static content (e.g. jquery, font-awesome, etc.) or use WebJars
  * Maven project archetype or other project setup tool
  * Debug-mode use of external tools like request.bin or other http test endpoints
  
