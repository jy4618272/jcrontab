/**
 *  This file is part of the jcrontab package
 *  Copyright (C) 2001-2002 Israel Olalla
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
package org.jcrontab.web;  

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import org.jcrontab.Crontab;
import org.jcrontab.log.Log;
/**
 * @author $Author: iolalla $
 * @version $Revision: 1.20 $
 */
public class loadCrontabServlet extends HttpServlet {
	
    private static Crontab crontab = null;
        /** Refer to Servlet Javadoc
         * This method is invoked by the Servlet container
		 * When the app-server starts.
         * @param config The ServletConfig
         */ 
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		
		try
		{
			System.out.print("Working?...");
            		process();
			System.out.println("OK");
		}
		catch (Exception e)
		{
			throw new ServletException(e);
		}
	}
	 /** 
	  * This method  starts the Crontab and lets the system
	  * Continue without wasting more resources.
	  * This method can receive the config File as a variable in web.xml
	  */        
	public void process() {

			    String propz = "jcrontab.properties";
	       		int iFrec = 6;
				//String path = getServletConfig().getServletContext()
				//								.getRealPath(".");
			    //System.out.println("Real Path: " + path);
				String props = getServletConfig()
								.getInitParameter("PROPERTIES_FILE");
								
				int freq = Integer.parseInt(getServletConfig()
								.getInitParameter("REFRESH"));
				
				if (props == null) props = propz;
				if (freq == 0) freq = iFrec;
			    crontab = Crontab.getInstance();
			
			try {
				ShutdownHook();
				crontab.init(props,freq);
			} catch (Exception e) {
				Log.error(e.toString(), e);
			}
    }
	
    /**
	 * This method seths a ShutdownHook to the system
	 *  This traps the CTRL+C or kill signal and shutdows 
	 * Correctly the system.
	 * @throws Exception
	 */ 
	 public static void ShutdownHook() throws Exception {
             Runtime.getRuntime().addShutdownHook(new Thread() {         
	 	public void run() {
			System.out.println("Shutting down...");
			// stops the system in 200 miliseconds :-)
			crontab.uninit(200);
			System.out.println("Stoped");
				}
			});
    }
}
