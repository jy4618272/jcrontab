/*
 *  This file is part of the jcrontab package
 *  Copyright (C) 2001 Israel Olalla
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free
 *  Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 *  MA 02111-1307, USA
 *
 *  For questions, suggestions:
 *
 *  iolalla@yahoo.com
 *
 */
package org.jcrontab;


import java.util.Calendar;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Date;
import java.util.Vector;
import java.util.Properties;
import java.io.*;

import org.jcrontab.data.CrontabEntryException;
import org.jcrontab.data.CrontabEntryDAO;
import org.jcrontab.data.CrontabEntryBean;

/** 
 * Implements a process that interprets a con-table and generates the events
 * described in that table
 * @author iolalla
 * @version 0.01
 */

public class Cron extends Thread
{
    private static final String GENERATE_TIMETABLE_EVENT = "gen_timetable";
	
    private Crontab crontab;
	
    private int iFrec;
	
    private static int minute = 60000;
	
    public String strFileName = "crontab";
	
	public static Properties prop = null;
	
    public static CrontabBean[] eventsQueue;
	
    public static CrontabEntryBean[] crontabEntryArray;
	
    /**
     * Constructor of a Cron. This one doesn't receive any parameters to make 
     * it easier to build an instance of Cron
     */
    public Cron() {
        crontab = Crontab.getInstance();
        iFrec = 60;
    }
    
    /**
     * Constructor of a Cron
     * @param cront The  Crontab that the cron must call to generate
     * new tasks
     * @param iTimeTableGenerationFrec Frecuency of generation of new time table entries.
     */
    public Cron(Crontab cront, int iTimeTableGenerationFrec) {
        crontab = cront;
        iFrec = iTimeTableGenerationFrec;
    }
    
	
    
    /** 
     * Initializes the crontab, reading tasks from configuration file
     * @throws CrontabEntryException Error parsing tasks configuration file entry
     * @throws FileNotFoundException Tasks configuration file not found
     * @throws IOException Error reading tasks configuration file
     */    
    public static void init() throws Exception {
        //crontabEntryArray = readCrontab();
    }
    
    /** 
     * Initializes the crontab, reading tasks from configuration file
     * @param strFileName Name of the tasks configuration file
     * @throws CrontabEntryException Error parsing tasks configuration file entry
     * @throws FileNotFoundException Tasks configuration file not found
     * @throws IOException Error reading tasks configuration file
     */    
    public void init(Properties prop) throws Exception {
      //crontabEntryArray = readCrontab(prop);
	  this.prop = prop;
    }
	
    /** 
     * Runs the Cron Thread.
     */    
    public void run() {
        int counter = 0;
        try {
			// Waits until the next minute to begin
       		waitNextMinute();
        	// Generates events list
       		generateEvents();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Infinite loop, this thread will stop when the jvm is stopped (it is
        // a daemon).
        while(true) {
            CrontabBean nextEv = null;
            nextEv = eventsQueue[counter];
            
            long intervalToSleep = nextEv.getTime() - System.currentTimeMillis();
            // System.out.println("intervalToSleep :" + intervalToSleep);
            if(intervalToSleep > 0) {
                // Waits until the next event
                try {
                    synchronized(this) {
                        wait(intervalToSleep);
                    }
                } catch(InterruptedException e) {
                    // Waits until the next minute to begin
                    waitNextMinute();
                    // Generates events list
                    generateEvents();
                    // Continues loop
                    continue;
                }
            }
            counter++;
            // If it is a generate time table event, does it.
            if(nextEv.getClassName().equals(GENERATE_TIMETABLE_EVENT)) {
                generateEvents();
				counter=0;
            }
            // Else, then tell the crontab to create the new task
            else {
                crontab.newTask(nextEv.getClassName(), nextEv.getMethodName(), 
				nextEv.getExtraInfo());
            }
        }
    }

    private void waitNextMinute() {
        // Waits until the next minute
        long tmp = System.currentTimeMillis();
        if(tmp % minute != 0) {
            long intervalToSleep = ((((long)(tmp / minute))+1) * minute) - tmp;
            // Waits until the next minute
            try {
                synchronized(this) {
                    wait(intervalToSleep);
                }
            } catch(InterruptedException e) {
                // Waits again
                waitNextMinute();
            }
        }        
    }
    

   /**
    * Reads the cron-table and converts it to the internal representation
    * @param strFileName Name of t 	        he tasks configuration file
    * @throws CrontabEntryException Error parsing tasks configuration file entry
    *
    */
         
   public static CrontabEntryBean[] readCrontab() throws Exception {
       CrontabEntryDAO.init();
       crontabEntryArray = CrontabEntryDAO.getInstance().findAll();
       return crontabEntryArray;
   }
   
   /**
    * Reads the cron-table and converts it to the internal representation
    * @param strFileName Name of the tasks configuration file
    * @throws CrontabEntryException Error parsing tasks configuration file entry
    *
    */
         
   public static CrontabEntryBean[] readCrontab(Properties prop) throws Exception {
       
       CrontabEntryDAO.init(prop);
       crontabEntryArray = CrontabEntryDAO.getInstance().findAll();
       return crontabEntryArray;
   }

    /**
     * Generates new time table entries (for new events).
     */
    public void generateEvents() {
        try {
			if (prop != null)  {
				crontabEntryArray = readCrontab(prop);
			} else {
				crontabEntryArray = readCrontab();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
        Vector lista1 = new Vector();
        CrontabEntryBean entry;
        // Rounds the calendar to the previous minute
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(((long)(System.currentTimeMillis() / 60000))
                * 60000));
        //System.out.println("Caņendario: "  + cal);
        for(int i=0; i<iFrec; i++) {
            for(int j=0; j<crontabEntryArray.length; j++) {
                entry = crontabEntryArray[j];
                if(entry.equals(cal)) {
                        CrontabBean ev = new CrontabBean();
                        ev.setId(j);
						ev.setCalendar(cal);
						ev.setTime(cal.getTime().getTime());
						ev.setClassName(entry.getClassName());
						ev.setMethodName(entry.getMethodName());
						ev.setExtraInfo(entry.getExtraInfo());
            	        lista1.add(ev);
						//System.out.println(ev);
                }
            }
            cal.add(Calendar.MINUTE, 1);
        }
        // The last event is the new generation of the event list
        CrontabBean ev = new CrontabBean();
		ev.setCalendar(cal);
		ev.setTime(cal.getTime().getTime());
		ev.setClassName(GENERATE_TIMETABLE_EVENT);
		ev.setMethodName("");
        lista1.add(ev);
        //int lastEvent = (crontabEntryArray.length + 1);
        eventsQueue = new CrontabBean[lista1.size()];
        for (int i = 0; i < lista1.size() ; i++) {
            eventsQueue[i] = (CrontabBean)lista1.get(i);
        }
    }
}
