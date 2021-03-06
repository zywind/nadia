/*
Natural Dialogue System (NADIA)
Copyright 2012-2016 Markus M. Berg

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package net.mmberg.nadia.processor;

import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import net.mmberg.nadia.processor.dialogmodel.Dialog;
import net.mmberg.nadia.processor.exceptions.RuntimeError;
import net.mmberg.nadia.processor.manager.DialogManager;
import net.mmberg.nadia.processor.store.DialogStore;
import net.mmberg.nadia.processor.ui.*;


public class NadiaProcessor extends UIConsumerFactory{

	
	private final static Logger logger = Logger.getLogger("nadia"); 
	private static boolean init=false;
	private static NadiaProcessorConfig config = NadiaProcessorConfig.getInstance();
	private static String default_dialog=config.getProperty(NadiaProcessorConfig.DIALOGUEDIR)+"/"+"dummy2.xml"; //default dialogue
	private static UserInterface ui;
	private static Date startedOn;
	
	/**
	 * @param args
	 */
	@SuppressWarnings("static-access")
	public static void main(String[] args) {
			
		Class<? extends UserInterface> ui_class=ConsoleInterface.class; //default UI
		String dialog_file=default_dialog; //default dialogue
		
		//process command line args
		Options cli_options = new Options();
		cli_options.addOption("h", "help", false, "print this message");
		cli_options.addOption(OptionBuilder.withLongOpt( "interface" )
                .withDescription( "select user interface" )
                .hasArg(true)
                .withArgName("console, rest")
                .create("i"));
		cli_options.addOption("f", "file", true, "specify dialogue path and file, e.g. -f /res/dialogue1.xml");
		cli_options.addOption("r", "resource", true, "load dialogue (by name) from resources, e.g. -r dialogue1");
		cli_options.addOption("s", "store", true, "load dialogue (by name) from internal store, e.g. -s dialogue1");
		
		CommandLineParser parser = new org.apache.commons.cli.BasicParser();
		try {
			CommandLine cmd = parser.parse(cli_options, args);
			
			//Help
			if(cmd.hasOption("h")){
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("nadia", cli_options, true);
				return;
			}
			
			//UI
			if(cmd.hasOption("i")){
				String interf=cmd.getOptionValue("i");
				if(interf.equals("console")) ui_class=ConsoleInterface.class;
				else if (interf.equals("rest")) ui_class=RESTInterface.class;
			}
			
			//load dialogue from path file
			if(cmd.hasOption("f")){
				dialog_file="file:///"+cmd.getOptionValue("f");
			}
			//load dialogue from resources
			if(cmd.hasOption("r")){
				dialog_file=config.getProperty(NadiaProcessorConfig.DIALOGUEDIR)+"/"+cmd.getOptionValue("r")+".xml";
			}
			//load dialogue from internal store
			if(cmd.hasOption("s")){
				Dialog store_dialog=DialogStore.getInstance().getDialogFromStore((cmd.getOptionValue("s")));
				store_dialog.save();
				dialog_file=config.getProperty(NadiaProcessorConfig.DIALOGUEDIR)+"/"+cmd.getOptionValue("s")+".xml";
			}
		
		} catch (ParseException e1) {
			logger.severe("NADIA: loading by main-method failed. "+e1.getMessage());
			e1.printStackTrace();
		}
		
			
		//start Nadia with selected UI
		default_dialog=dialog_file;
		NadiaProcessor nadia = new NadiaProcessor();
		try {
			ui = ui_class.newInstance();
			ui.register(nadia);
			ui.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private NadiaProcessor(){
		if (!init) init();
	}
	
	//for external loading (war)
	public static void loadByWar(UserInterface uinterf){
		default_dialog=config.getProperty(NadiaProcessorConfig.DIALOGUEDIR)+"/"+"dummy2.xml";
		NadiaProcessor nadia = new NadiaProcessor();
		try {
			ui = uinterf;
			ui.register(nadia);
			//ui.start();
		} catch (Exception e) {
			logger.severe("NADIA: loading by war failed. "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static Dialog getDefaultDialog(){
		return Dialog.loadFromPath(default_dialog);
	}
	
	public static String getDefaultDialogPathAndName(){
		return default_dialog;
	}
	
	public static String getUIType(){
		return ui.getClass().getSimpleName();
	}
	
	public static Date getStartedOn(){
		return startedOn;
	}
	
	public static boolean isInit(){
		return init;
	}
	
	@Override
	public UIConsumer create() throws RuntimeError{
		return new DialogManager();
	}
	
	@Override
	public UIConsumer create(Dialog d) throws RuntimeError {
		return new DialogManager(d);
	}
		
	private void init(){
		init=true;
		startedOn = new Date();
		//format logging
		logger.setUseParentHandlers(false);	 
		CustomFormatter fmt = new CustomFormatter();
		Handler ch = new ConsoleHandler();
		ch.setFormatter(fmt);
		logger.addHandler(ch);
		
		logger.setLevel(Level.INFO);
	}
	
	public static Logger getLogger(){
		return logger;
	}

	
	public class CustomFormatter extends Formatter {

		public String format(LogRecord record) {
			
			StringBuffer sb = new StringBuffer();
			sb.append(record.getLevel().getName());
			sb.append(" (");
			sb.append(record.getSourceClassName().substring(record.getSourceClassName().lastIndexOf('.')+1));
			sb.append("): ");
			sb.append(formatMessage(record));
			sb.append("\n");

			return sb.toString();
		}
	}


}
