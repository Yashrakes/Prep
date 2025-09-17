supports standard log level 
log messages with timestamp, level, content
multiple output destination
configuration mechanism for log level and output destination
extensibility for new log level and output destination


chain of responsibility for log processor

strategy pattern for log appender Allows runtime selection of Log appenders (Console, File etc.)

singleton pattern for logger instance

public enum loglevel {
debug(1);
info(2);
error(3);

private final int value;
Loglevl(int value){
this.value = value;
}
public int getvalue(){
return value;

}
public boolean isgreaterthenorequal(Loglevel other){
return this.value >= other.value;
}
}


private logmessage {
priavte final lloglelevel ;
private final stirng message
private final long timestamp;

public logmessage(Loglevel level., string message ){
this., level = level ;
this.messag e = message 
this. timestamp= system . cuutrenttimemikls();
}
@override
public string tostring(){
return "[" + level + "] " + timestamp + " - " + message;
}
}


abstract clas log handler{
	publci stattic final int info =1;
	 public Looghandler lg;
	 public logappender apppender
	 
	
}

















