package test_camera;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.io.FileUtils;

public class Test {
	
	private static int nameRef;
	
	/////////////////PARAMETERS/////////////////

	static int NB_CAPTURE = 1600;
	static int TIMER_INTERVAL = 4500; //milliseconds
	static int MODE = 2; //  0:Monothread   1:Multithread   2:MonothreadV2   3:ModuleFunctions
	static int MAX_ATTEMPTS = 10; // Nombre maximum de tentatives pour une requête

	/////////////////TOOLS/////////////////
	
	public static StringBuffer query(String urlString, String data) throws IOException {
		
		//Le contenu de la réponse POST
		StringBuffer content = new StringBuffer();
		
		
		int attemptCount = 0; // Compteur de tentatives
		while(attemptCount<MAX_ATTEMPTS) {
			
			try {
				URL url = new URL(urlString);
			
				/* Ouvre une connection avec l'object URL */
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		
				//Methode POST
				connection.setRequestMethod("POST");
				connection.setDoOutput(true);
		        connection.setRequestProperty("Content-Type", "application/json"); //
		        
		        // Encodez les données POST à envoyer
		        byte[] postDataBytes = data.getBytes("UTF-8");
		
				/* Écrit les données dans la requête POST */
		        DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
		        wr.write(postDataBytes);
		        wr.flush();
		        wr.close();
		
				/* Utilise BufferedReader pour lire ligne par ligne */
				BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		
				//La ligne courante
				String inputLine;
		
				/* Pour chaque ligne dans la réponse POST */
				while ((inputLine = in.readLine()) != null) { //attend bien la fin de la requête
				 content.append(inputLine);
				}
		
				//Ferme BufferedReader
				in.close();
				
		        break; // Sortir de la boucle si la requête réussit
			
			} catch (IOException e) {
				attemptCount++; // Augmenter le compteur de tentatives
		        if (attemptCount == MAX_ATTEMPTS) {
		            // Si le nombre maximum de tentatives est atteint, arrêter la boucle
		            throw e; // Lancer l'exception pour signaler l'échec de la requête
		        }
		        // Attendre avant de réessayer la requête
		        try {
		            Thread.sleep(2000);
		        } catch (InterruptedException ex) {
					ex.printStackTrace();
		        }
				e.printStackTrace();
			}
		}
		
		System.out.println("nombre de tentatives : " + (attemptCount + 1));
		return content;
	}
	
	public static String getSessionId(String response) {
		String res = response.split("SID_")[1];
		res = res.substring(0, 4);
		return "SID_" + res;
	}
	
	public static String getFingerprint(String response) {
		String res = response.split("FIG_")[1];
		res = res.substring(0, 4);
		return "FIG_" + res;
	}
	
	public static String getURL(String response) {
		String res = response.split("_latestFileUrl\":\"")[1];
		res = res.split("\",\"_batteryState")[0];
		return res;
	}
	
	public static int getNameRef(String url) {
		String name = url.split("/100RICOH/R")[1].substring(0, 7);
		int nameRef = 0;
		try {
			nameRef = Integer.parseInt(name);
		} catch (NumberFormatException e) {
		  System.out.println("Impossible de convertir la chaîne en entier.");
		}
		return nameRef;
	}
	
	/////////////////FUNCTIONS/////////////////
		
	public static String initCamera(boolean highResolution) {
		nameRef = 0;
		String sessionId = "";
		try {
		    //begin session and get sessionId
		    StringBuffer response = query("http://192.168.1.1/osc/commands/execute", "{\"name\" : \"camera.startSession\" }");
		    sessionId = getSessionId(response.toString());
		    
		    //set options
	    	response = query("http://192.168.1.1/osc/commands/execute", "{\"name\": \"camera.setOptions\",\"parameters\": {\"sessionId\": \"" + sessionId + "\" ,\"options\": {\"clientVersion\": 2}}}");

	    	//set resolution
		    if (highResolution) {
		    	response = query("http://192.168.1.1/osc/commands/execute", "{\"name\": \"camera.setOptions\",\"parameters\": {\"options\": {\"fileFormat\": {\"type\": \"jpeg\",\"width\": 5376,\"height\": 2688}}}}");
		    } else {
		    	response = query("http://192.168.1.1/osc/commands/execute", "{\"name\": \"camera.setOptions\",\"parameters\": {\"options\": {\"fileFormat\": {\"type\": \"jpeg\",\"width\": 2048,\"height\": 1024}}}}");
		    }
		} catch (IOException e) {
		    e.printStackTrace();
		}
		return sessionId;
	}
	
	//A TESTER
	//TODO mettre un attribut gerant le nameRef pour éviter d'avoir à attendre le traitement de photo
	public static String takePicture(){
		long start = System.currentTimeMillis();
		String imageRef = "";
		try {
		    StringBuffer response = query("http://192.168.1.1/osc/state", "{}");
		    String lastImageUrl = getURL(response.toString());
		
		    if (!lastImageUrl.equals("")) {
		        response = query("http://192.168.1.1/osc/commands/execute", "{\"name\" : \"camera.takePicture\"}");
		        if (nameRef == 0) {
		        	nameRef = getNameRef(lastImageUrl);
		        }
		        nameRef += 1;
		        imageRef = String.format("%07d", nameRef);
		    } else {
		        String url = "";
		        response = query("http://192.168.1.1/osc/commands/execute", "{\"name\" : \"camera.takePicture\"}");
		        while (url.equals("")) {
		            response = query("http://192.168.1.1/osc/state", "{}");
		            url = getURL(response.toString());
		        }
		        nameRef = getNameRef(url);
		        imageRef = String.format("%07d", nameRef);
		    }
		} catch (IOException e) {
		    e.printStackTrace();
		}
		
		long stop = System.currentTimeMillis();
		long elapsed = stop - start;
		System.out.println("Photo " + imageRef + " : " + elapsed + "ms");
		return imageRef;
	}
	
	//?
	public static String[] takeNPictures(int n, int time_interval){
		String[] imageRefs = new String[n];
		
		return imageRefs;
	}
	
	//?
	public static String[] takePicturesDuringTimer(int timer, int time_interval){
		String[] imageRefs = new String[10];
		
		return imageRefs;
	}
	
	public static void downloadPicture(String imageRef){
		long start = System.currentTimeMillis();
		
		//download image in pictures directory
		try {
		    URL url = new URL("http://192.168.1.1/files/035344534c303847803aea0cf9010c01/100RICOH/R" + imageRef + ".JPG");
		    File download = new File("./pictures/R" + imageRef + ".JPG");
		    FileUtils.copyURLToFile(url, download);
		} catch (IOException e) {
		    throw new RuntimeException(e);
		}
		long stop = System.currentTimeMillis();
		long elapsed = stop - start;
		System.out.println("Download " + imageRef + " : " + elapsed + "ms");
	}
		
	public static void downloadPictures(String[] imageRefs){
		for (int i=0; i<imageRefs.length; i++){
		    downloadPicture(imageRefs[i]);
		}
	}
	
	public static void clearPictures() {
		File directory = new File("./pictures");
        
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
	}
	
	public static void delete(String[] imageRefs) {
		try {
			String content = "{\"name\" : \"camera.delete\", ,\"parameters\": {\"fileUrls\": [";
			for (int i=0; i<imageRefs.length-1; i++) {		
				content += "\"http://192.168.1.1/files/035344534c303847803aea0cf9010c01/100RICOH/R" + imageRefs[i] + ".JPG\", ";
			}
			content += "\"http://192.168.1.1/files/035344534c303847803aea0cf9010c01/100RICOH/R" + imageRefs[imageRefs.length -1] + ".JPG\" ]}}";
			StringBuffer response = query("http://192.168.1.1/osc/commands/execute", content);
			System.out.println(response);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	/////////////////TESTS/////////////////

	
	//Monothreaded execution
	public static void monoThreadTest(String sessionId) {
		try {
		//get fingerprint
		StringBuffer response = query("http://192.168.1.1/osc/state", "{}");
//		System.out.println(response);
		String fingerprint = getFingerprint(response.toString());
		
		for(int i=0; i<NB_CAPTURE; i++) {		
				
			//take picture
			response = query("http://192.168.1.1/osc/commands/execute", "{\"name\" : \"camera.takePicture\"}");
			
			//wait image treatment
			String newfingerprint = fingerprint;
		    while (newfingerprint.equals(fingerprint)) {
				response = query("http://192.168.1.1/osc/checkForUpdates", "{\"stateFingerprint\" : \"" + fingerprint + "\" }");
				newfingerprint = getFingerprint(response.toString());
				Thread.sleep(100);
		    }
		    
		    //get URL
			response = query("http://192.168.1.1/osc/state", "{}");
//			System.out.println(response);
			String imageUrl = getURL(response.toString()); //
			System.out.println(imageUrl);
			
			//download image in pictures directory
			URL url = new URL(imageUrl);
			File download = new File("./pictures/" + i + "-picture");
			FileUtils.copyURLToFile(url, download);
			Thread.sleep(TIMER_INTERVAL);
		}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		} 
	}

	//Multithreaded execution
	public static void multiThreadTest(String sessionId) {
		
		Worker[] workers = new Worker[NB_CAPTURE];
		
		for(int i=0; i<NB_CAPTURE; i++) {
			Worker w = new Worker("w"+i, sessionId);
			workers[i] = w; 
			w.start();
			try {
				Thread.sleep(TIMER_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		for(int i=0; i<NB_CAPTURE; i++) {
			try {
				workers[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	//Monothreaded execution v2
	public static void monoThreadTestV2(String sessionId) {
		try {			
//			StringBuffer response = query("http://192.168.1.1/osc/state", "{}");
////			System.out.println(response);
//			String fingerprint = getFingerprint(response.toString());	
			
			long start = System.currentTimeMillis();
			//take picture
			StringBuffer response = query("http://192.168.1.1/osc/commands/execute", "{\"name\" : \"camera.takePicture\"}");
			System.out.println("photo 0");
			
//			//wait image treatment
//			String newfingerprint = fingerprint;
//		    while (newfingerprint.equals(fingerprint)) {
//				response = query("http://192.168.1.1/osc/checkForUpdates", "{\"stateFingerprint\" : \"" + fingerprint + "\" }");
//				newfingerprint = getFingerprint(response.toString());
//				//Thread.sleep(100);
//		    }

			response = query("http://192.168.1.1/osc/state", "{}");
			String lastImageUrl = getURL(response.toString());
			
			while (lastImageUrl.equals("")) {
				response = query("http://192.168.1.1/osc/state", "{}");
				lastImageUrl = getURL(response.toString());
			}
			
			int nameRef = getNameRef(lastImageUrl);
			
			long stop = System.currentTimeMillis();
			long elapsed = stop - start;
			
			if(elapsed < TIMER_INTERVAL) { //if the init take less time than timer_interval
				Thread.sleep(TIMER_INTERVAL - elapsed);
				stop = System.currentTimeMillis();
				elapsed = (stop - start);
			}	

			System.out.println(elapsed + "ms -> photo 1");
			
			for(int i=1; i<NB_CAPTURE; i++) {	
				start = System.currentTimeMillis();
				
				//take picture
				response = query("http://192.168.1.1/osc/commands/execute", "{\"name\" : \"camera.takePicture\"}");
				if (i==(NB_CAPTURE-1)) { //we don't want to wait after the last capture
					break;
				}
				
				//wait
				long tmp =  System.currentTimeMillis();
				long elapsed_tmp = tmp - start;
				if (elapsed_tmp < TIMER_INTERVAL) {
					Thread.sleep(TIMER_INTERVAL - elapsed_tmp ); //wait timer_interval - capture_time
				} else {
					//?
				}
				
				stop = System.currentTimeMillis();
				elapsed = stop - start;

				System.out.println(elapsed + "ms ->  photo " + (i+1));		

			}

			//download all pictures
//			for(int i=0; i<NB_CAPTURE; i++) {
//				String imageRef = String.format("%07d", nameRef + i);
//	
//				start = System.currentTimeMillis();
//				//download image in pictures directory
//				URL url = new URL("http://192.168.1.1/files/035344534c303847803aea0cf9010c01/100RICOH/R" + imageRef + ".JPG");
//				File download = new File("./pictures/R" + imageRef + ".JPG");
//				FileUtils.copyURLToFile(url, download);
//				stop = System.currentTimeMillis();
//				elapsed = (stop - start);
//				System.out.println("telechargement " + i + " : " + elapsed + "ms");
//			}
			
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		} 
	}
	
	public static void moduleFunctionTest(){
		clearPictures();
		
		String[] imageRefs = new String[NB_CAPTURE];
		for (int i=0; i<NB_CAPTURE; i++) {
			imageRefs[i] = takePicture();
			try {
				Thread.sleep(TIMER_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		downloadPictures(imageRefs);
//		delete(imageRefs);
	}
	
	
	/////////////////MAIN/////////////////

	
	public static void main(String[] args) {
		
		long start = System.currentTimeMillis();
		
		//begin session and get sessionId
		String sessionId = initCamera(false);

		//Test choice
		if (MODE == 0) {
			monoThreadTest(sessionId);
		} else if (MODE == 1) {
			multiThreadTest(sessionId);
		} else if (MODE == 2){
			monoThreadTestV2(sessionId);
		} else {
			moduleFunctionTest();
		}
		
		//clearPictures();
		
		long stop = System.currentTimeMillis();
		long elapsed = (stop - start) / 1000;
		System.out.println("END");
		System.out.println("Durée d'exécution totale : " + elapsed + "s");

	}

}
