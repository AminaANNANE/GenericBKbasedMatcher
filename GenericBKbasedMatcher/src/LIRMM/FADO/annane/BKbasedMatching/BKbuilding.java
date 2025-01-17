package LIRMM.FADO.annane.BKbasedMatching;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.semanticweb.owl.align.Alignment;

import eu.sealsproject.platform.res.tool.api.ToolBridgeException;
import eu.sealsproject.platform.res.tool.api.ToolException;
import fr.inrialpes.exmo.align.parser.AlignmentParser;




public class BKbuilding {


	URL source;
	URL target;
	public static String sourceIRI;
	Map<String,String> ExistingAlignments;
	String[] BkOntologies;
	Map<String,String> BkOntologiesCodes=new HashMap<String,String>();
	public static String sourceAcronym;
	public static boolean sourceNeedInterface=false; 
	public static HashMap<String, String > codeInterface=new HashMap<>();
	public static HashMap<String, String> sourceElements ;

	public static HashMap<String, String> ontologyAcronym;
	int code;
	
	public Map<String, String> getBkOntologiesCodes() {
		return BkOntologiesCodes;
	}
	

	Map<String, TreeSet<Noeud>> globalGraph=new HashMap<String, TreeSet<Noeud>>();
	Map<String, TreeSet<Noeud>> BkGraph=new HashMap<String, TreeSet<Noeud>>();

	/**
	 * Constructor
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public BKbuilding() throws FileNotFoundException, IOException, URISyntaxException
	{
		globalGraph.clear();
		BkGraph.clear();
		ExistingAlignments=getExistingAlignments();
		File BkOntologiesFolder = new File(C.BkOntologiesFolderPath); 
		BkOntologies = BkOntologiesFolder.list(); 
		getOntologyCodes();
	}
	/**
	 * The main function
	 * @return
	 * @throws Exception
	 */
	public Map<String, TreeSet<Noeud>> BuildBK() throws Exception
	{	
		Fichier folder=new Fichier(C.BkAlignmentsFolderPath);
		folder.deleteFile();
		generateBkFromOneFolder();
		matchOntologyToBKontologies();
		chargerBKMappingsFromFolder();
		if(C.ExistingMappingsPath!=null)chargerBK_Mappings();
		Model ontologySourceModel=JenaMethods.LoadOntologyModelWithJena(source);
		selectSubGraph(ontologySourceModel);
		createOwlFile2();
		

		return BkGraph;
	}
	
	
	
	static String getAcronym(String IRI)
	{
		String res;
		res=IRI.substring(IRI.lastIndexOf("/")+1);
		if(res.contains("."))res=res.substring(0, res.indexOf("."));
		res=res.toLowerCase();
		System.out.println("Voici l'acronyme: "+res);
		return res;
	}
	/**
	 * This function supposes that the global graph is already loaded and selects the subgraph for a set of uris
	 * @param URIs
	 * @return
	 * @throws Exception
	 */
	public Map<String, TreeSet<Noeud>> BuildEnrichedBK(TreeSet<String> URIs) throws Exception
	{
		long debut=System.currentTimeMillis();	
		chargerBKMappingsFromFolder();
		if(C.ExistingMappingsPath!=null)chargerBK_Mappings();
		selectSubGraph(URIs);
		enrichWithChildrenAndFathers();
		createOwlFile2();
		long time=System.currentTimeMillis()-debut;
		C.executionTime.add("BKbuild "+(time)+"ms");
		return BkGraph;
	}
	/**
	 * Select a sub graph from a set of uris
	 * @param URIs: the set of URIS that will help for the selection
	 * @throws Exception
	 */
	/* ***************************************************************************************** */
	void selectSubGraph(TreeSet<String> URIs) throws Exception
	{
	long debut =System.currentTimeMillis();
	 TreeSet<String> sourceElements = JenaMethods.getFirstSelectedConcepts(URIs, sourceIRI);
	 TreeSet<String> untreated=new TreeSet<>(), treated=new TreeSet<>();
	 untreated.addAll(sourceElements);
	 TreeSet<Noeud> l;
	 String e,m; 
	 BkGraph.clear();
	 while (untreated.size()>0) 
	 {
		 e=untreated.first();
		 untreated.remove(e);
		 treated.add(e);
		 l = globalGraph.get(e);

		 if(l!=null)
		 {	
			 BkGraph.put(e,l);
			 for (Noeud noeud : l) {
				m=noeud.ontology+C.separator+noeud.code;
				if(!treated.contains(m))
				{
					untreated.add(m);
				}
			}
		 }
	}
	 System.out.println("[selectSubGraph] La taille globale du graph: "+globalGraph.size());
	 System.out.println("[selectSubGraph] La taille du graph BK: "+BkGraph.size());
	// globalGraph.clear();
	// enrichWithChildrenAndFathers();

	 long time=System.currentTimeMillis()-debut;
	 C.executionTime.add("size subGraph: "+BkGraph.size());
	 C.executionTime.add("selectSubGraph "+(time)+"ms");
	}
	
	
	public static HashMap<String,String> loadOntologyElementsForSelection(Model ontology,String acronym, boolean needInterface) throws Exception
	{
		HashMap<String,String>  ontologyUriCode = new HashMap<String,String> ();
		ResultSet res=JenaMethods.ExecuteQuery(C.prefix+"select ?x where {?x a owl:Class}", ontology);
		String uri;
		while (res.hasNext()) 
		{
		
            uri=res.next().get("x").toString();
            if(uri.contains("http"))
            { 
            	String code=Fichier.getUriCode(uri,needInterface);
                if(needInterface)
                {
                    if(codeInterface.containsKey(acronym+C.separator+code))
                	{
                     code=codeInterface.get(acronym+C.separator+code);
                     ontologyUriCode.put(acronym+C.separator+code,uri);
                	}
                }
                else
                {
                    ontologyUriCode.put(acronym+C.separator+code,uri);	
                }

            }
		}	
		return ontologyUriCode;
	}
	
	/**
	 * 
	 * @param ontologySource
	 * @param obo
	 * @throws Exception
	 */
	void selectSubGraph(Model ontologySource,boolean obo) throws Exception
	{
	long debut =System.currentTimeMillis();
	 sourceElements = loadOntologyElementsForSelection(ontologySource,sourceAcronym,sourceNeedInterface);
	 System.out.println(sourceElements.keySet());
	 TreeSet<String> untreated=new TreeSet<>(), treated=new TreeSet<>();
	 untreated.addAll(sourceElements.keySet());
	 TreeSet<Noeud> l;
	 String e,m; 
	 BkGraph.clear();
	 while (untreated.size()>0) 
	 {

		 e=untreated.first();
		 //System.out.println(e);
		 untreated.remove(e);
		 treated.add(e);
		 l = globalGraph.get(e);

		 if(l!=null)
		 {	
			 BkGraph.put(e,l);
			 for (Noeud noeud : l) {
				m=noeud.ontology+C.separator+noeud.code;
				if(!treated.contains(m))
				{
					untreated.add(m);
				}
			}
		 }
	}
	 System.out.println("La taille globale du graph: "+globalGraph.size());
	 System.out.println("La taille du graph BK: "+BkGraph.size());
	// globalGraph.clear();
	// enrichWithChildrenAndFathers();

	 long time=System.currentTimeMillis()-debut;
	 C.executionTime.add("size subGraph: "+BkGraph.size());
	 C.executionTime.add("selectSubGraph "+(time)+"ms");
	}
	/* ***************************************************************************************** */
	void selectSubGraph(Model ontologySource) throws Exception
	{
	long debut =System.currentTimeMillis();
	 TreeSet<String> sourceElements = JenaMethods.loadOntologyElementsForSelection(ontologySource,sourceIRI);
	 TreeSet<String> untreated=new TreeSet<>(), treated=new TreeSet<>();
	 untreated.addAll(sourceElements);
	 TreeSet<Noeud> l;
	 String e,m; 
	 int cpt = 0;
	 BkGraph.clear();
	 while (untreated.size()>0) 
	 {
		 e=untreated.first();
		 untreated.remove(e);
		 treated.add(e);
		 l = globalGraph.get(e);

		 if(l!=null)
		 {	
			 cpt=cpt+l.size(); 
			 BkGraph.put(e,l);
			 for (Noeud noeud : l) {
				m=noeud.ontology+C.separator+noeud.code;
				if(!treated.contains(m))
				{
					untreated.add(m);
				}
			}
		 }
		 
	}
	 System.out.println(cpt);
	 System.out.println("La taille globale du graph: "+globalGraph.size());
	 System.out.println("La taille du graph BK: "+BkGraph.size());
	// globalGraph.clear();
	// enrichWithChildrenAndFathers();

	 long time=System.currentTimeMillis()-debut;
	 C.executionTime.add("size subGraph: "+BkGraph.size());
	 C.executionTime.add("selectSubGraph "+(time)+"ms");
	}
	/* *********************************************************************************************** */
	 void enrichWithChildrenAndFathers() throws MalformedURLException, URISyntaxException
	 {

			long debut =System.currentTimeMillis();
			Model ontology=null;
			String fathers="", children;
			ResultSet res;
			String newUri;
			QuerySolution sol;		
			TreeSet<String> conceptList=new TreeSet<>();
			conceptList.addAll(BkGraph.keySet());
			HashMap<String, String> ontologyConcepts=new HashMap<>();
			String o,uri,values;
			for (String oc : conceptList) 
			 {	 
				 o=oc.substring(0,oc.indexOf(C.separator));
				 uri=oc.substring(oc.indexOf(C.separator)+1);
				 if(!ontologyConcepts.keySet().contains(o))
				 {
					 values="<"+uri+"> ";
				 }
				 else
				 {
					 values=ontologyConcepts.get(o)+"<"+uri+"> "; 
				 }
				 ontologyConcepts.put(o,values);
			  }	
			
			//localize ontology paths
			HashMap<String, String> ontologyPath=new HashMap<>();	
			File BkOntologiesFolder =new File(C.BkOntologiesFolderPath);
			for (String filePath : BkOntologiesFolder.list()) 
			{
				File file=new File(C.BkOntologiesFolderPath+File.separator+filePath);
				String ontologyURI=JenaMethods.getOntologyUri(file.toURI().toURL());
				ontologyPath.put(ontologyURI, file.getAbsolutePath());
			}
			
			
			System.out.println("before "+BkGraph.size());
			for (String ontoURI : ontologyPath.keySet()) {
				String path=ontologyPath.get(ontoURI);
			 	ontology=JenaMethods.LoadOntologyModelWithJena(path);
			 	/* ********************** looking for fathers *********************************** */
				String Query=C.prefix+"SELECT ?c ?f  where {?c <"+org.apache.jena.vocabulary.RDFS.subClassOf+"> ?f} ";
				if(ontologyConcepts.get(ontoURI)!=null){
				Query=Query+ " VALUES ?c  {"+ontologyConcepts.get(ontoURI)+"}";
				res = JenaMethods.ExecuteQuery(Query, ontology);
				 if(res!=null)
				 {
					 while(res.hasNext())
					 {
						 sol = res.next();
						 
					     
				    	 String fatherURI=sol.get("f").toString();
				    	 if(fatherURI.contains("http"))
				    	 {
							 String fatherConcept=ontoURI+C.separator+fatherURI;
							 
							 String childURI=sol.get("c").toString();
						     String childConcept=ontoURI+C.separator+childURI;
								
	                         
	                         if(!BkGraph.containsKey(fatherConcept))
	                         {BkGraph.put(fatherConcept, new TreeSet<Noeud>());}
	                         
	                         Noeud child=new Noeud(childURI, ontoURI, 0.2,"child");
	                         Noeud father=new Noeud(fatherURI, ontoURI, 0.2,"father");
	                          
	                         BkGraph.get(childConcept).add(father);
	                         BkGraph.get(fatherConcept).add(child); 
				    	 }

				 }	
			}
		      /* ********************** looking for children *********************************** */
					Query=C.prefix+"SELECT ?c ?f  where {?c <"+org.apache.jena.vocabulary.RDFS.subClassOf+"> ?f} ";
					Query=Query+ " VALUES ?f  {"+ontologyConcepts.get(ontoURI)+"}";
					res = JenaMethods.ExecuteQuery(Query, ontology);
					
					 if(res!=null)
					 {
						 while(res.hasNext())
						 {
							 sol = res.next();
							 String childURI=sol.get("c").toString();
							 if(childURI.contains("http")){
						     String childConcept=ontoURI+C.separator+childURI;
						     
					    	 String fatherURI=sol.get("f").toString();
							 String fatherConcept=ontoURI+C.separator+fatherURI;
								                         
	                         if(!BkGraph.containsKey(childConcept))
	                         {BkGraph.put(childConcept, new TreeSet<Noeud>());}
	                         
	                         Noeud child=new Noeud(childURI, ontoURI, 0.2,"child");
	                         Noeud father=new Noeud(fatherURI, ontoURI, 0.2,"father");
	                          
	                         BkGraph.get(childConcept).add(father);
	                         BkGraph.get(fatherConcept).add(child);
						 }
					 }	
				}
				}
						 
	 ontology.close();
	 }

						long time=System.currentTimeMillis()-debut;
						System.out.println("after "+BkGraph.size());
						C.executionTime.add("enrichWithFamily "+(time)+"ms");
	 }
	
	
	/* ************************************************************************************************ */
	void createOwlFile2() throws Exception
	{
		long debut =System.currentTimeMillis();
		Model ontology=null;
		String requete1="", requete2;
		ResultSet res;
		String newUri;
		QuerySolution sol;
		
		//supprimer l'ancien builtBk s'il existe
		Fichier fichier=new Fichier(C.BuiltBkPath);		
		fichier.deleteFile();
		

		//IRIFactory iriFactory = IRIFactory.semanticWebImplementation();
		//IRI iri = iriFactory.create("http://BK.rdf"); // always works
		// create an empty Model
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);;
		model.setNsPrefix("skos", SKOS.getURI());
		model.setNsPrefix("owl", OWL.NS);
		model.setNsPrefix("", "http://BK.owl");
		model.createOntology("http://BK.owl");
		
	
		TreeSet<String> conceptList=new TreeSet<>();
		conceptList.addAll(BkGraph.keySet());
		
		File BkOntologiesFolder =new File(C.BkOntologiesFolderPath);
		String values="";
		String o;
		for (String filePath : BkOntologiesFolder.list()) 
		{
		 	            String path=C.BkOntologiesFolderPath+filePath;
						File file=new File(path);
					 	String ontologyURI=JenaMethods.getOntologyUri(file.toURI().toURL());
					 	System.out.println("*****************************Nouvelle BK ontologie: "+ontologyURI);

						ontology=JenaMethods.LoadOntologyModelWithJena(file.toURI().toURL());
						 {	 //chercher tous les concepts de cet ontologies
							 String uri;
							 values="";
							 int numberConcept=0;
							 for (String oc : conceptList) 
							 {	 
								 o=oc.substring(0,oc.indexOf(C.separator));
								 if(o.equalsIgnoreCase(ontologyURI))
								 {
									numberConcept++;
									uri=oc.substring(oc.indexOf(C.separator)+1);
									values=values+"<"+uri+"> ";
								 }
							  }				
                    int cpt=0;
					 /* ****************************** Requete1: retrieve prefLabs ********************************* */
                    
					 requete1=C.prefix+"SELECT ?x ?y  where {?x "+C.prefLabs+" ?y} ";
					 requete1=requete1+ " VALUES ?x  {"+values+"}";
					 res = JenaMethods.ExecuteQuery(requete1, ontology);	
					 if(res!=null)
					 {
						 while(res.hasNext())
						 {
							 cpt++;
							 sol = res.next();
							 String codeU=sol.get("x").toString();
							 if(conceptList.contains(ontologyURI+C.separator+codeU))
							 { 
								 String label=sol.get("y").toString();
								 newUri=codeU+"/"+BkOntologiesCodes.get(ontologyURI);
								 model.createClass(newUri).addProperty(SKOS.prefLabel, label);
							 }
							 else System.out.println("[CreateOwlFile] BE carfeul");
						 }
					 }
					 if(cpt!=numberConcept)System.out.println("[CreateOwlFile] the number of concepts is: "+numberConcept+" found is: "+cpt);
					 /* *************************** Requete2: retrieve synonyms ************************************ */
					 requete2=C.prefix+"select distinct ?x ?y where {?x "+C.synonyms+" ?y}";
					 requete2=requete2+ " VALUES ?x  {"+values+"}";
					 
					 res = JenaMethods.ExecuteQuery(requete2, ontology);	
					 if(res!=null)
					 {
						 while(res.hasNext())
						 {
						 cpt++;
						// System.out.println(cpt);
						 
						 sol = res.next();
						 String codeU=sol.get("x").toString();
						 if(conceptList.contains(ontologyURI+C.separator+codeU))
						 { 
							 String label=sol.get("y").toString();
							 newUri=codeU+"/"+BkOntologiesCodes.get(ontologyURI);
							 model.createClass(newUri).addProperty(SKOS.altLabel, label);}
						 else System.out.println("[CreateOwlFile2] Be careful: "+ontologyURI+C.separator+codeU);
						 }
					 }						 
				 ontology.close();
				 }
				}
					FileWriter out = null;
					try {
						  // XML format - long and verbose
						  Fichier.returnFolder(C.BkFolderPath);
						  out = new FileWriter( C.BuiltBkPath);
						  model.write( out, "RDF/XML" );			 		
						}
					finally {
					  if (out != null) {
					    try {out.close();} 
					    catch (IOException ignore) {}
					  }
					}
					long time=System.currentTimeMillis()-debut;
					C.executionTime.add("createOWLfile2 "+(time)+"ms");
	}
	/* ********* charger les mappings ******************** */
	  void chargerBK_Mappings() throws IOException
	{
		long debut =System.currentTimeMillis();
		String uri_source,ontologySource, uri_target,ontologyTarget,score;
		File f = new File(C.ExistingMappingsPath); 
		//rendre cette partie automatique pour tous les fichiers RDF et CSV existants
		BufferedReader reader = new BufferedReader(new FileReader(f)); 
		String line = null; 
		while ((line = reader.readLine()) != null) 
		{
			try{
			//System.out.println("[chargerBK_Mappings]"+line);
			StringTokenizer lineParser = new StringTokenizer(line, ";"); 
			uri_source=lineParser.nextElement().toString();
			ontologySource=lineParser.nextElement().toString();
			uri_target=lineParser.nextElement().toString();
			ontologyTarget=lineParser.nextElement().toString();
			score =lineParser.nextElement().toString();		

			Noeud map=new Noeud(uri_target,ontologyTarget, Double.parseDouble(score));
			
			if(globalGraph.containsKey(ontologySource+C.separator+uri_source))globalGraph.get(ontologySource+C.separator+uri_source).add(map);
			else
			{
				TreeSet<Noeud> liste=new TreeSet<>();
				liste.add(map);
				globalGraph.put(ontologySource+C.separator+uri_source,liste);
			}
			 
			map= new Noeud(uri_source,ontologySource, Double.parseDouble(score));
			if(globalGraph.containsKey(ontologyTarget+C.separator+uri_target))globalGraph.get(ontologyTarget+C.separator+uri_target).add(map);
			else
			{
				TreeSet<Noeud> liste=new TreeSet<>();
				liste.add(map);
				globalGraph.put(ontologyTarget+C.separator+uri_target,liste);
			}
		}
			catch(java.lang.NumberFormatException e)
			{e.printStackTrace();}
			catch(java.util.NoSuchElementException e)
			{e.printStackTrace();}
			}
		long time=System.currentTimeMillis()-debut;
		C.executionTime.add("chargerBKmappings "+(time)+"ms");
		int liste_concept_number =0;
		for (String k:globalGraph.keySet())
		{
			liste_concept_number = liste_concept_number + globalGraph.get(k).size();
		}
		System.out.println("[chargerBK_Mappings] globalGraph: "+globalGraph.size());
	}
	
	/* ***************************************************************************** */
	 void chargerBKMappingsFromFolder() throws Exception
	{
		long debut =System.currentTimeMillis();
		TreeSet<String> mappings=new TreeSet<>();
		File BkAlignmentsFolder = new File(C.BkAlignmentsFolderPath); 
		String[] BkAlignmentsList = BkAlignmentsFolder.list();
		for (String a : BkAlignmentsList) 
		{
			if(a.contains(".rdf"))
			{
				TreeSet<String> mappingsA = Fichier.loadOAEIAlignment(C.BkAlignmentsFolderPath+a);
				mappings.addAll(mappingsA);
			}
		}
		
		int mapping_number = mappings.size();
		String uri_source,ontologySource, uri_target,ontologyTarget,score; 
		for (String line : mappings) 	
		{
			StringTokenizer lineParser = new StringTokenizer(line, ","); 
			uri_source=lineParser.nextElement().toString();
			ontologySource=lineParser.nextElement().toString();
			uri_target=lineParser.nextElement().toString();
			ontologyTarget=lineParser.nextElement().toString();
			score =lineParser.nextElement().toString();			
			Noeud map=new Noeud(uri_target,ontologyTarget, Double.parseDouble(score));
			if(globalGraph.containsKey(ontologySource+C.separator+uri_source))
				{
				globalGraph.get(ontologySource+C.separator+uri_source).add(map);
				}
			else
			{
				TreeSet<Noeud> liste=new TreeSet<>();
				liste.add(map);
				globalGraph.put(ontologySource+C.separator+uri_source,liste);
			}
			 
			map= new Noeud(uri_source,ontologySource, Double.parseDouble(score));
			if(globalGraph.containsKey(ontologyTarget+C.separator+uri_target))globalGraph.get(ontologyTarget+C.separator+uri_target).add(map);
			else
			{
				TreeSet<Noeud> liste=new TreeSet<>();
				liste.add(map);
				globalGraph.put(ontologyTarget+C.separator+uri_target,liste);
			}
		}
		int cpt = 0;
		for (String k:globalGraph.keySet())
		{
			cpt = cpt +globalGraph.get(k).size();
		}
		System.out.println("[chargerBKMappingsFromFolder] globalGraph: "+globalGraph.size());
	}
	/**
	 * This function matches the source ontology to the BK ontologies
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws ToolBridgeException 
	 * @throws ToolException 
	 */
	    void matchOntologyToBKontologies() throws FileNotFoundException, IOException, URISyntaxException, ToolException, ToolBridgeException
	   {  
		  for(int j=0;j<BkOntologies.length;j++)
		  { 
			File f=new File(C.BkOntologiesFolderPath+BkOntologies[j]);
			String targetIRI=JenaMethods.getOntologyUri(f.toURI().toURL());
			generateBKalignment(source, f.toURI().toURL(), sourceIRI, targetIRI);			
		  }
	   }
	    void getOntologyCodes() throws FileNotFoundException, IOException, URISyntaxException
	   {	code=1;	  
		  for(int j=0;j<BkOntologies.length;j++)
		  { 
			File f=new File(C.BkOntologiesFolderPath+BkOntologies[j]);
			String targetIRI=JenaMethods.getOntologyUri(f.toURI().toURL());
			
			//ajouter à la liste bkontologiescodes
			BkOntologiesCodes.put(targetIRI, "o"+code);
			BkOntologiesCodes.put("o"+code, targetIRI);
			code++;
			
		  }
	   }
	/**
	 * This funtion matches all ontologies within a given folder between each other
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws ToolBridgeException 
	 * @throws ToolException 
	 */
	 void generateBkFromOneFolder() throws FileNotFoundException, IOException, URISyntaxException, ToolException, ToolBridgeException
	{	  
		  for(int i=0;i<BkOntologies.length;i++)
		  { 
			for(int j=i+1;j<BkOntologies.length;j++)
			  { 
				File sourceFile=new File(C.BkOntologiesFolderPath+File.separator+BkOntologies[i]);
				String sourceIRI=JenaMethods.getOntologyUri(sourceFile.toURI().toURL());
				File targetFile=new File(C.BkOntologiesFolderPath+File.separator+BkOntologies[j]);
				String targetIRI=JenaMethods.getOntologyUri(targetFile.toURI().toURL());
				generateBKalignment(sourceFile.toURI().toURL(), targetFile.toURI().toURL(), sourceIRI, targetIRI);
			  }
		  }
	} 
	/**
	 * This function generates an alignment between two ontologies
	 * @param source The ontology source URL
	 * @param target The ontology target URL
	 * @param sourceIRI The source IRI
	 * @param targetIRI The target IRI
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws ToolBridgeException 
	 * @throws ToolException 
	 */
	void generateBKalignment(URL source,URL target, String sourceIRI, String targetIRI) throws IOException, URISyntaxException, ToolException, ToolBridgeException
	{
		if(ExistingAlignments.containsKey(sourceIRI+C.separator+targetIRI))
		{
			  System.out.println("[generateBKalignment] super il existe");
			  String fileName=ExistingAlignments.get(sourceIRI+C.separator+targetIRI);
			  File srcFile=new File (C.alignmentsRepositoryFolderPath+fileName);
			  File destFile=new File(C.BkAlignmentsFolderPath+fileName);
		      org.apache.commons.io.FileUtils.copyFile(srcFile, destFile);
		}
		else{
			Matching matching=new Matching(source,target);
			String alignmentName=getRandomName(C.BkAlignmentsFolderPath);
			URL res=matching.matchOntologies(C.BkAlignmentsFolderPath+File.separator+alignmentName+".rdf");		
			System.out.println("lurl du fichier resultat: "+res);

		}
		
		}
	/**
	 * 
	 * @param path the path of the alignment
	 * @return an array that includes two elements the first is the IRI of the source ontology and the second is the IRI of the target ontology
	 * @throws Exception the API alignment is used to parse alignments, it may throw parsing exceptions
	 */
      ArrayList<String> getAlignmentOntologies(String path) throws Exception
	{
		  AlignmentParser aparser = new AlignmentParser(0);
		  Alignment al = aparser.parse( new File( path ).toURI() );
		  String o1=al.getOntology1URI().toString();
		  String o2=al.getOntology2URI().toString();
		  ArrayList<String> l=new ArrayList<>();
		  l.add(o1);
		  l.add(o2);
		  return l;
	} 
      /**
       * generate a random file name that does not exist in the Folder
       * @param FolderPath
       * @return a random fileName
       */
String getRandomName(String FolderPath)
{
	  String randomName = RandomStringUtils.randomAlphabetic(10).toUpperCase();
      // Randomly generate the scenario name and check if a dir already got this name
      while (new File(FolderPath + File.separatorChar + randomName).exists()) {
        randomName = RandomStringUtils.randomAlphabetic(10).toUpperCase();
      }
      return randomName;
	}
/**
 * 
 * @return existing alignment list (o1�o2,fileName.rdf)
 */
Map<String, String> getExistingAlignments()
{
	Map<String,String> existingAlignments=new HashMap<String,String>();
	File alignmentsRepositoryFolder = Fichier.returnFolder(C.alignmentsRepositoryFolderPath);
	String[] alignments = alignmentsRepositoryFolder.list();  
	  for (String a : alignments) 
	  {
		  if(a.contains(".rdf"))
		  try
		  {
			  ArrayList<String> l = getAlignmentOntologies(C.alignmentsRepositoryFolderPath+a);
			  existingAlignments.put(l.get(0)+C.separator+l.get(1), a );
		  }
		  catch(Exception e)
		  {
			  System.out.println("alignment: "+a+"that throws the exception");
			  e.printStackTrace();
		  }
	  }
	  return existingAlignments;
	}
public static void loadConcepts(String folderPath,String acronym) throws Exception{
	
		File folder=new File(folderPath); 
		String[] liste = folder.list();
		for (String fichier : liste) {
			String path=folderPath+File.separator+fichier;
			File f=new File(path);
			BufferedReader reader = new BufferedReader(new FileReader(f)); 
			String line;
			while((line=reader.readLine())!=null){
				StringTokenizer lineParser=new StringTokenizer(line, ",");
				String ontologyCode = lineParser.nextToken();
				String ontology= lineParser.nextToken();
				String prefLabCode=lineParser.nextToken();
				if(prefLabCode!=null)
				{
					if(ontology.equalsIgnoreCase(acronym))
					codeInterface.put(ontology+C.separator+prefLabCode,ontologyCode);
					}
				else throw new Exception("prefLabCode VIDE");
			}
			reader.close();
		}


}

}
