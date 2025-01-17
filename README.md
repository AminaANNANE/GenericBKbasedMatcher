# GBM: Generic BK based Matcher

This project is the code of a Generic Background-knowledge based ontology Matcher (**GBM**). GBM has been implemented during my thesis that aimed to enhance ontology matching using external knowledge resources. GBM is the implementation of our Background-knowledge based ontology matching approach presented in [1]. It uses any direct matcher as a black box. In addition, it offers several parameters that allows to have various configurations according to the user needs.

We used this framework to participate in the OAEI 2017.5 compaign, with YAM-BIO, which is GBM with YAM++ [2] as a direct matcher and two biomedical ontologies as background knowledge DOID and UBERON. Our results are available here https://goo.gl/A496ug

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites
The **direct matcher** instanciated in the *Parameters* class has to implement a static function align which takes as input the URL of ontologies to align, and returns the URL of the generated alignment:

        URL alignment=align (URL ontology1, URL ontology2) 
        
The generated alignment should be stored in RDF format with the API alignment to be parsed correctly. Systems that have participated in the OAEI campaigns, may use GBM directly without any adaptation. Indeed, OAEI participants have to wrap their tools as SEALS packages, and the wrapping procedure includes the implementation of the function Align. [3]

**Background knowledge resources.** GBM can exploit two Background-Knowledge (BK) resource types: (i) ontologies, and (ii) existing mappings. Since we use Jena API to load and parse ontologies, all ontology formats supported by Jena API, such as RDF and OWL, are supported by GBM. However, the formats of the background knowledge ontologies should be supported by the direct matcher too. Existing mappings may be provided in two formats:
1. RDF format generated by the alignment API.
2. CSV format where each row has a value for the different attributes illustrated in the file obo.CSV available in the folder BK/ExistingMappings.

**Alignment repository.** GBM has its own alignment repository. Indeed, the idea is to avoid aligning the same pair of ontologies with the same matcher more than once to gain in efficiency. Hence, before performing any matching task, GBM verifies if an alignment between the input ontologies exists to reuse it. Otherwise, GBM generates the required alignment and stores it in the alignment repository. Currently, the alignment repository is a folder, available on the path *BK/alignmentsRepository*, which contains RDF files, where each file stores an alignment between two ontologies.

**BK building.** In our approach, we build a knowledge resource from the BK ontologies and the existing mappings (please see [1] for more detail). The *BKbuilding* class implements this approach. The building method needs the following parameters declared in the *Parameters* class: 
1. matcher: the direct matcher that will be used for matching BK ontologies and anchoring later.
2. sourceOntology: URL of the source ontology.
3. BKselectionInternalExploration: a boolean that indicates if GBM should genrate mappings of other relations than equivalence.
4. BKselectionExplorationRelations: specifies the relations that GBM should generate in addition to equivalence.
5. BKselectionExplorationLength: the internal exploration length of BK ontologies. A value of 1 means to return children and parents of the already selected concepts, while a value of 2 means to return parents, grandparents, children, grandchildren
In addition, we added a new method that allows to enrich the built BK with internal relations extracted from the preselected ontologies.

**Mapping derivation.** GBM provides two mapping derivation strategies. The first one assumes that the Neo4j is already installed and the Built BK is stored as a Neo4j graph database. It consists in searching all possible paths between source and target concepts. This derivation strategy is complete, i.e., it finds all possible candidate mappings, however it is not scalable for large built BK graphs --when having a large number of BK ontologies -- and it depends on Neo4j. We tried to address these issues by implementing another derivation algorithm, which represents the second derivation strategy.
Finally, mapping derivation offers the following parameters (static variables in *Parameters* class):
1. derivationStrategies: it takes one of the two possible values "neo4j" or "specific_algo"
2. derivationMaxPathLength: the built BK has a graph format, the derivation process looks for paths between source and target concepts. This parameter limits the length of these paths. The length of a given path is the number of its edges (by default it is 4). 

**Mapping selection.** GBM implements the two mapping selection methods that presented in [1]: ML based selection and Rule based selection methods. When choosing the ML based selection, the user has to provide one or several datasets, such that each dataset is a folder in the *BK/DataSets* folder, which contains two ontologies and their validated alignment. These datasets will be used for training the classifier. When choosing te rule based selection, the user may specify a threshold value to select only the mappings that have a score equivalent to or higher than this threshold value. To summarize, for mapping selection, GBM offers the following parametrs:

1. mappingSelectionStrategies: it may takes one of the two values "Rule_based" or "ML_based".
2. mappingSelectionThreshold: it takes a value between 0 and 1 (by default, it is 0)
3. training dataSets

The ML-based selection uses the RandomForest algorithm.

**Semantic verification.** Currently, GBM reuses the LogMapRepair module to verify the consistency of the generated alignment. The semantic verification is optional and the user may disable it using the *Semantic verification* parameter.


The list of all parameters may be found in the *Parameters* class.

Please, before running the code be sure that all libraries and resources required by the direct matcher are referenced.

The framework offers two derivation strategies (Neo4j and specific_algo). When using the Neo4j strategy, the Neo4j graph database should be installed and variables driver and session in the Parameters class should be initialized.
DataSets for ML based selection

### Installing





[1] https://www.sciencedirect.com/science/article/pii/S1570826818300179?via%3Dihub.

[2] http://www.websemanticsjournal.org/index.php/ps/article/view/483.

[3] http://oaei.ontologymatching.org/2017/.
