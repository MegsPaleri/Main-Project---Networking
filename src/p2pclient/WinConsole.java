/*
 * P2P Challenge Task 2006 - P2P Streaming Application
 * 
 * Copyright (c) 2006 P2P Challenge Groups, University of Zurich
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA 
 * 
 */

package p2pclient;

import com.sun.java_cup.internal.runtime.Symbol;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;



public class WinConsole extends Thread{
	
	Runtime runtime;
	Process process;
	
	boolean started = false;
        String file;
    public WinConsole(String File) {
            file=File;
    }
        
	public void run(){
            startConsole(file);
        }
	public void startConsole(String file){
		System.out.println ("-WinConsole started");
                try{
                    Thread.sleep(2000);
                    String path = "C:\\Program Files\\VideoLAN\\VLC\\vlc";
                    if(!startPlayer(path, file)){

                            JFileChooser chooser = new JFileChooser();
                            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                                                                    ".exe files", "exe");
                            chooser.setFileFilter(filter);
                            int returnVal = chooser.showOpenDialog(chooser);
                            if(returnVal == JFileChooser.APPROVE_OPTION) {
                               System.out.println("You chose to open this file: " +
                                    chooser.getSelectedFile());
                               startPlayer(chooser.getSelectedFile().getAbsolutePath(), file);
                            }


                    }
                    
                }catch(Exception e){
                        System.err.println(e);
                }
                                    
	}
	private boolean startPlayer(String path,String file){
                String param = " "+file;
		String cmd = path+param;
		 
		try {
		    runtime = Runtime.getRuntime();
		    process = runtime.exec(cmd);
		    started = true;
                    return true;
		}
		catch (Exception e) {
                      return false;                       
		}
        }
	public void shutdown() {
		if(started) process.destroy();
	}
        
}