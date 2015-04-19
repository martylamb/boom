# Boom - a collection of helpers for building simple web applications on the Spark framework

Boom is a simple convention-over-configuration library that wraps around [Spark](http://sparkjava.com) to allow simple web applications to be built extremely quickly.  Its primary goal is to match my own development style and reduce boilerplate.  Hopefully you'll find it useful, too.

Just use com.martiansoftware.boom.Boom in place of spark.Spark for additional functionality.  Spark is used behind the scenes by Boom.  You can still call Spark directly from the same application if you like.


## BoomRoute

When registering Routes (e.g., Spark's get() and post() methods), you can alternately specify a BoomRoute, which takes no arguments.  Spark's Request and Response objects for the current Thread are made available by Boom's request() and response() methods, so your BoomRoute implementations can do this (assuming static method App.doThing()):

```java
get("/thing", App::doThing);
```

instead of this:
```java
get("/thing", (req,rsp) -> App.doThing(req,rsp));
```

## BoomResult

Routes (and BoomRoutes) can continue return any type of Object for Spark to render to the client.  But if you return a BoomResult then a number of things are taken care of for you automatically.

The BoomResponse() constructor can take a String, InputStream, File, or URL as an argument.  In each case you will get what you expect; the InputStream will be copied to the client, or the File's contents, or even the contents at the URL.  Since Files and URLs may have file extensions, Boom will set the appropriate MimeType as well.

Type helpers and a fluent interface allow a single call to performa multiple tasks like:
```java
private static Object doThing() {
	return new BoomResult("{'thing':'done'}").json();
}
```

## Debug Mode

If the environment variable or system property BOOM_DEBUG is "1", then certain behaviors are modified so support development (described here and there below).

## Static Content

Static content is automatically configured to load from the classpath under /static-content.  For Maven projects, just put them into src/main/resources/static-content and it's all set up for you.  When your jar is bundled and delivered, static content will automatically be packaged and included by Maven.

If running in Debug Mode, then static content is instead automatically configured to load from the filesystem under src/main/resources/static-content.  This allows reloading of content from the filesystem during development without restarting your application.

## Templates

You can use Spark's built-in template functionality, but Boom provides helpers for use with [DumbTemplates](https://github.com/martylamb/dumbtemplates).  Template can be obtained from Boom via template(templateName).

Behavior is similar to that for static content: templates are automatically configured to load from the classpath under /templates.  For Maven projects, just put them into src/main/resources/templates and it's all set up for you.  When your jar is bundled and delivered, templates will be automatically packaged and included by Maven.

If running in Debug Mode, then templates are instead automatically configured to load from the filesystem under src/main/resources/templates.  This allows reloading of templates from the filesystem during development without restarting your application.


