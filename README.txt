{
  type: 'war',  // later things like zip or .tar.gz I suppose. possibly even a 'dir' meaning directory
  url: 'target/test-1.0.war',
  deploy: 'http://tomcat:tomcat@localhost:8080/manager/deploy?path=/test&update=true',
  config: { // war specific templating variables, using doT v1.0 passing in config as the it
  preDeploy: [], // default to nothing if not defined
  postDeploy: [], // default to nothing if not defined
  templateExtension: '.dot', // defaults to .dot so this does not need to be declared but can be overwritten if needed.
}

This config will see it is a 'war' type and thus apply a templating to all the immediate content within the war file. It
will not check jars or other items inside the war itself for template files. It will locate anything ending with .dot in
a case insensitive manner and attempt to create a version of the file without the .dot extension with the results of the
.dot file having been processed.

The system will check the url to grab the war file that it will need in order to do the above templating. it may be
anything that is supported by java such as http, https, or file as examples. By default not putting any kind of protocol
will result in it being a file:// protocol. It will also at this point create a hash (md5, sha1, sha256?) of the file
in order to better understand if updates have taken place. Obviously if it is a url it might be nice to respect the
response codes which indicate that no changes have taken place. However, I suspect most systems will not tell you that
information accurately. Thus to know whether or not a redeployment needs to take place, a check should be done locally
as well.

After the .war file has been processed, the system will check the deploy object for deployment instructions. This could
be either a null meaning do nothing with the file which would be a bit pointless probably but better safe then sorry. It
could also be a webapp deployment definition such as to tomcat as is the case in the above example. It might also be in
the future perhaps a request to simply sftp or copy to some other location on the filesystem.

IDEA: If these configuration files could be stored in a git repository then you have very easy rollback capabilities by
simply rolling back to the previous git version and then redeploying again.

1) download url saving to uuid.<type> in a temp folder
2) scan all files ending with <templateExtension> within the uuid.<type> file or folder
3) process each matching template using 'config' with a non-extension version of it
4) invoke each preDeploy step sequentially through a shell. TODO design to allow scala, groovy, jython, and jruby later.
5) transfer binary to url via http/s, scp, ftp, or file cp
6) invoke each postDeploy step sequentially through a shell.

