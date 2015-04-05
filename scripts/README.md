## A collection of scripts used to generate portions of boom source code.

You probably don't need to run these; the generated sources are under version control.

### updateBoomJava

Helper script to update the main Boom class to wrap delegate various http method calls to Spark.

This only needs to be run if new http method signatures are added to Spark.

### updateMimeTypes

Helper script to generate the MimeType.java class.  This script reads the local /etc/mime.types file and turns it into an enum.

This only needs to be run if new mime types of interest are added to /etc/mime.types.

### updateStatusCodes

Reads the iana status code registry and creates a .properties-like file mapping status numbers to (English) descriptive text.


