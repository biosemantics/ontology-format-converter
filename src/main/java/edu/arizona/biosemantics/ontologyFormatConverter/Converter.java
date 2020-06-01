package edu.arizona.biosemantics.ontologyFormatConverter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import uk.ac.manchester.cs.jfact.JFactFactory;

//use formatConverter install (https://www.screencast.com/t/B5GuLPagFDJ) in configuration to produce converter.jar and lib. 
public class Converter {
	public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

	//convert carex.owl to carex.ttl
	//make sure carex.owl at input location is refreshed with updates. That is, /save api endpoint is called.
	public static void main(String[] args) {
		
		String error ="";
		//first, call /save api to commit in-memory changes in the carex.owl to the desk
		OkHttpClient client = new OkHttpClient();

		String json = "{\"user\":\"\", "+
                    	"\"ontology\":\"carex\"}";
	    RequestBody body = RequestBody.create(json, JSON);
	    Request request = new Request.Builder()
	      .url("http://shark.sbs.arizona.edu:8080/save?")
	      .post(body)
	      .build();
	    Response response = null;
	    try{
	    	response = client.newCall(request).execute();
	    	if(!response.body().string().equals("true")){
		    	error += "http://shark.sbs.arizona.edu:8080/save? failed"+System.lineSeparator();
		    }
	    }catch(Exception e){
	    	StringWriter sw = new StringWriter();
	    	PrintWriter pw = new PrintWriter(sw);
	    	e.printStackTrace(pw);
	    	
	    	error += "http://shark.sbs.arizona.edu:8080/save? failed:" +sw.toString()+System.lineSeparator();
	    }
	    
	    //if conditions are all met, do the conversion
	    if(! error.isEmpty()){
	    	//do nothing, wait for the error to be printed later	
	    }else if(!args[0].equals("/home/hongcui/charaparser-web/ontologies/carex.owl")){
	    	///home/hongcui/charaparser-web/ontologies/carex.owl
	    	error += "input file location is not /home/hongcui/charaparser-web/ontologies/carex.owl";
	    }else{
			//args 0: input file path
			//args 1: output file path
			System.out.println("input file:"+args[0]);
			System.out.println("output file:"+args[1]);
	
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			try{
				OWLOntology carex = manager.loadOntologyFromOntologyDocument(new File(args[0]));
				//reason over carex ontology, type by type
				JFactFactory reasonerFactory = new JFactFactory();
				OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(carex);
				for(InferenceType type : InferenceType.values()){
					try{
						reasoner.precomputeInferences(type);
					}catch(Exception e){
						error += e.getMessage()+"\n";
						//do nothing, conflict detected or time out on this
					}
				}
				//write out the reasoned carex ontology out in turtle format
				FileOutputStream fos = new FileOutputStream(new File(args[1]));
				manager.saveOntology(reasoner.getRootOntology(), new TurtleDocumentFormat(),
						fos);
			}catch(Exception e){
				error += e.getMessage();
			}
	    }

		if(error.trim().isEmpty()){
			System.out.println("OK, file converted");
		}else{
			//write out error
			try{
			    BufferedWriter writer = new BufferedWriter(new FileWriter("error.txt"));
			    writer.write(error);
			    writer.close();
				System.out.println("Errors encountered during conversion, see error.txt for details");
			}catch(Exception e){
				System.out.println("Errors encountered but writing error message to error.txt failed");
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}



