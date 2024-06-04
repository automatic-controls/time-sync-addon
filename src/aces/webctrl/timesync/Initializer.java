/*
  BSD 3-Clause License
  Copyright (c) 2022, Automatic Controls Equipment Systems, Inc.
  Contributors: Cameron Vogt (@cvogt729)
*/
package aces.webctrl.timesync;
import javax.servlet.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import org.springframework.scheduling.support.*;
import com.controlj.green.core.comm.connection.IPCommands;
import com.controlj.green.addonsupport.*;
import com.controlj.green.core.data.ServiceUserSession;
public class Initializer implements ServletContextListener {
  private volatile Thread thread;
  private volatile boolean go = true;
  /** Contains basic information about this addon. */
  public volatile static AddOnInfo info = null;
  /** The name of this addon */
  private volatile static String name;
  /** Used for logging status messages. */
  private volatile static FileLogger logger;
  /** Path to the private data folder for this addon. */
  private volatile static Path data;
  private volatile static Path dataFile;
  private volatile static String expr = null;
  private volatile static CronSequenceGenerator cron = null;
  private volatile static long nextRunTime = -1L;
  private final static AtomicBoolean running = new AtomicBoolean(false);
  private synchronized static boolean save(){
    ByteBuffer buf = ByteBuffer.wrap((expr==null?"":expr).getBytes(StandardCharsets.UTF_8));
    try(
      FileChannel out = FileChannel.open(dataFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    ){
      while (buf.hasRemaining()){
        out.write(buf);
      }
      return true;
    }catch(Throwable t){
      log(t);
      return false;
    }
  }
  private synchronized static boolean load(){
    try{
      if (Files.exists(dataFile)){
        setCronExpression(new String(Files.readAllBytes(dataFile), StandardCharsets.UTF_8));
      }
      return true;
    }catch(Throwable t){
      Initializer.log(t);
      return false;
    }
  }
  @Override public void contextInitialized(ServletContextEvent sce){
    info = AddOnInfo.getAddOnInfo();
    name = info.getName();
    logger = info.getDateStampLogger();
    data = info.getPrivateDir().toPath();
    dataFile = data.resolve("schedule");
    load();
    thread = new Thread(){
      @Override public void run(){
        while (go){
          try{
            Initializer.execIfReady();
            Thread.sleep(300000);
          }catch(InterruptedException e){}
        }
      }
    };
    thread.start();
  }
  @Override public void contextDestroyed(ServletContextEvent sce){
    try{
      go = false;
      thread.interrupt();
      thread.join();
    }catch(InterruptedException e){}
    save();
  }
  /**
   * Initiates this scheduled test if it is ready according to the cron expression scheduler.
   * @return whether the scheduled test executed.
   */
  public static boolean execIfReady(){
    final long nextRunTime = Initializer.nextRunTime;
    if (nextRunTime!=-1 && nextRunTime<=System.currentTimeMillis()){
      return exec();
    }else{
      return false;
    }
  }
  /**
   * Initiates this scheduled test.
   * @return whether the test executed.
   */
  public static boolean exec(){
    reset();
    if (running.compareAndSet(false,true)){
      try{
        log("Attempting to synchronize time...");
        try(
          ServiceUserSession s = new ServiceUserSession("TimeSync");
        ){
          new IPCommands(true).sendBroadcastTimesyncToAll(s);
        }
        log("Time synchronization successful.");
      }catch(Throwable t){
        log("Time synchronization failed.");
        log(t);
      }finally{
        running.set(false);
      }
      return true;
    }
    return false;
  }
  /**
   * Resets the next run time of this scheduled test.
   */
  public static void reset(){
    CronSequenceGenerator cron = Initializer.cron;
    if (cron==null){
      nextRunTime = -1;
    }else{
      try{
        nextRunTime = cron.next(new Date()).getTime();
      }catch(Throwable t){
        nextRunTime = -1;
      }
    }
  }
  /**
   * @return the cron expression used for scheduling purposes.
   */
  public static String getCronExpression(){
    return expr;
  }
  /**
   * Sets the cron expression used for scheduling purposes.
   * @return {@code true} on success; {@code false} if the given expression cannot be parsed.
   */
  public static boolean setCronExpression(String expr){
    if (expr.equals(Initializer.expr)){
      return true;
    }else{
      Initializer.expr = expr;
      try{
        cron = new CronSequenceGenerator(expr);
        return true;
      }catch(Throwable t){
        cron = null;
        return false;
      }finally{
        reset();
      }
    }
  }
  /**
   * @return the next run time of this test. If {@code -1}, then this test should not ever be auto-executed.
   */
  public static long getNext(){
    return nextRunTime;
  }
  /**
   * @return the next run time of this test as a {@code String}.
   */
  public static String getNextString(){
    long nextRunTime = Initializer.nextRunTime;
    return "Next Sync: "+(nextRunTime==-1?"None":Utility.getDateString(nextRunTime));
  }
  /** @return the name of this application. */
  public static String getName(){
    return name;
  }
  /**
   * Logs the given message.
   */
  public static void log(final String str){
    logger.println(str);
  }
  /**
   * Logs the given error.
   */
  public static void log(final Throwable t){
    logger.println(t);
  }
  /**
   * @return the directory where data should be saved.
   */
  public static Path getDataFolder(){
    return data;
  }
}