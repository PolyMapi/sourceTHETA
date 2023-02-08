package test_camera;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import org.apache.commons.io.FileUtils;




public class Worker extends Thread {
	public String name;
	public String sessionId;
	
	Worker(String name, String sessionId){
		this.name = name;
		this.sessionId = sessionId;
	}
	
	public StringBuffer query(String urlString, String data) throws IOException {
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

		//Le contenu de la réponse POST
		StringBuffer content = new StringBuffer();

		/* Pour chaque ligne dans la réponse POST */
		while ((inputLine = in.readLine()) != null) {
		 content.append(inputLine);
		}

		//Ferme BufferedReader
		in.close();
		return content;
	}
	
	public String getFingerprint(String response) {
		String res = response.split("FIG_")[1];
		res = res.substring(0, 4);
		return "FIG_" + res;
	}
	
	public String getURL(String response) {
		String res = response.split("_latestFileUrl\":\"")[1];
		res = res.split("\",\"_batteryState")[0];
		return res;
	}
	
	public void run() {
		//begin timer
		long start = System.currentTimeMillis();
		
		System.out.println("hello from worker " + name);
		
		try {
			
			//get fingerprint
			StringBuffer response = query("http://192.168.1.1/osc/state", "{}");
//			System.out.println(response);
			String fingerprint = getFingerprint(response.toString());
			

			//take picture
			response = query("http://192.168.1.1/osc/commands/execute", "{\"name\" : \"camera.takePicture\"}");
			
			//wait image treatment
			String newfingerprint = fingerprint;
		    while (newfingerprint == fingerprint) {
				response = query("http://192.168.1.1/osc/checkForUpdates", "{\"stateFingerprint\" : \"" + fingerprint + "\" }");
				newfingerprint = getFingerprint(response.toString());
//				try {
//					Thread.sleep(500);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
		    }
		    
		    //get URL
			response = query("http://192.168.1.1/osc/state", "{}");
//			System.out.println(response);
			String imageUrl = getURL(response.toString()); //
			System.out.println(imageUrl);
			
			//download image in pictures directory
			URL url = new URL(imageUrl);
			File download = new File("./pictures/" + name + "-picture");
			FileUtils.copyURLToFile(url, download);

		
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		System.out.println("bye from worker " + name);
		
		//end timer
		long stop = System.currentTimeMillis();
		long elapsed = (stop - start) / 1000;
		System.out.println("Durée d'exécution de " + name + ": " + elapsed + "s");
		
		
	}

}
