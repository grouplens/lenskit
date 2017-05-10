package org.lenskit.eval.traintest;

import com.google.common.base.Stopwatch;
import com.google.common.eventbus.Subscribe;
import org.lenskit.util.monitor.JobEvent;
import org.lenskit.util.monitor.TrackedJob;
import org.slf4j.Logger;
import java.net.Socket;
import java.util.Scanner;
import java.io.IOException;
import java.util.Date;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.net.*;

import java.io.*;

public class WebAppListener {
    public static Logger log;
    private static Socket socket;
    private static PrintStream ps;
    private static final String USER_AGENT = "Mozilla/5.0";

    public WebAppListener(Logger logger) throws Exception {
        log=logger;
        log.info("WebListener was created");
    }

    /**
     * EventStart is subscribed to the event bus and waits for it to send out a JobEvent.Started object.
     * The JobEvent information is then parsed into a JSON object and sent to the WebApp server
     * @param js : Job Event that has started
     * @throws Exception
     */
    @Subscribe
    public static void EventStart(JobEvent.Started js) throws Exception {
        try {
            String url = "http://localhost:3000";
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            TrackedJob job = js.getJob();
            TrackedJob parent = job.getParent();
            String jobString = CreateJSON(job, parent, 0);
            sendPostRequest(jobString, con);
        } catch(java.net.ConnectException e){

        }
    }

    /**
     * EventFinish is subscribed to the event bus and waits for it to send out a JobEvent.Finished object.
     * The JobEvent information is then parsed into a JSON object and sent to the WebApp server
     * @param jf : Job Event that has finished
     * @throws Exception
     */
    @Subscribe
    public static void EventFinish(JobEvent.Finished jf) throws Exception{
        try {
            String url = "http://localhost:3000";
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            TrackedJob job = jf.getJob();
            TrackedJob parent = job.getParent();
            String jobString = CreateJSON(job, parent, 2);
            sendPostRequest(jobString, con);
        } catch(java.net.ConnectException e){

        }
    }

    @Subscribe
    public static void EventFailed(JobEvent.Failed jf) throws Exception{
        try {
            String url = "http://localhost:3000";
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            String description = jf.getJob().getDescription();
            TrackedJob job = jf.getJob();
            TrackedJob parent = job.getParent();
            Throwable exception;
            exception = jf.getException();
            String jobString = CreateJSON(job, parent, 3);
            sendPostRequest(jobString, con);
        }catch(java.net.ConnectException e){

        }
    }

    /**
     * EventProgressUpdate is subscribed to the event bus and waits for it to send out a JobEvent.ProgressUpdate object.
     * The JobEvent information is then parsed into a JSON object and sent to the WebApp server
     * @param jp : Job Event that gives a progress update of the current job
     * @throws Exception
     */
    @Subscribe
    public static void EventProgressUpdate(JobEvent.ProgressUpdate jp) throws Exception{
        try {
            String url = "http://localhost:3000";
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            TrackedJob job = jp.getJob();
            TrackedJob parent = job.getParent();
            String jobString = CreateJSON(job, parent, 1);
            sendPostRequest(jobString, con);
        }catch(java.net.ConnectException e){

        }
    }

    /**
     * The JSON String is sent to the WebApp server
     * @param jobInfoJson : The JSON object that will be sent to the server
     * @param con : The HttpURLConnection to the server.
     * @throws Exception
     */

    private static void sendPostRequest(String jobInfoJson, HttpURLConnection con) throws Exception {

        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(jobInfoJson);
        wr.flush();
        wr.close();
        BufferedReader in = new BufferedReader( new InputStreamReader(con.getInputStream()));
        String output;
        StringBuffer response = new StringBuffer();

        while ((output = in.readLine()) != null) {
            response.append(output);
        }
        in.close();

    }

    /**
     *
     * @param job : The job that we are creating the json string for.
     * @param parent : The parent of the current job
     * @param typeOfUpdate : 0=Start 1=Progress 2=Finish 3=Failed
     * @return : Return the json string
     */
    private static String CreateJSON(TrackedJob job,TrackedJob parent, int typeOfUpdate){
        String data = "{";
        data= data.concat("\"id\":\"" + job.getUUID().toString() + "\",");
        data= data.concat("\"type\":\"" + job.getType() + "\",");
        if (job.getDescription()!=null){
            data= data.concat("\"description\":\"" + job.getDescription() + "\",");
        }
        else{
            data= data.concat("\"description\":\"null\",");
        }
        if (typeOfUpdate==0 || typeOfUpdate==1) {
            data = data.concat("\"completed\":\"0\",");
        }
        else if (typeOfUpdate==2){
            data = data.concat("\"completed\":\"1\",");
        }
        else if (typeOfUpdate==3){
            data = data.concat("\"completed\":\"2\",");
        }
        data= data.concat("\"expectedSteps\":\"" + job.getExpectedSteps() + "\",");
        data=data.concat("\"stepsFinished\":\"" + job.getStepsFinished() + "\",");
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        if (typeOfUpdate==0) {
            data = data.concat("\"startingTime\":\"" + timestamp.toString() + "\",");
        }
        else{
            data = data.concat("\"startingTime\":\"null\",");
        }
        if (typeOfUpdate == 0 || typeOfUpdate == 1 || typeOfUpdate == 3) {
            data = data.concat("\"finishingTime\":\"null\",");
        }
        else if (typeOfUpdate==2){
            data = data.concat("\"finishingTime\":\"" + timestamp.toString() + "\",");
        }
        if (parent.getUUID()!=null) {
            data = data.concat("\"parentID\":\"" + parent.getUUID() + "\",");
        }
        else{
            data = data.concat("\"parentID\":\"null\",");
        }
        data = data.concat("\"eventNumber\":\"" + typeOfUpdate + "\"}");
        return data;
    }
}
