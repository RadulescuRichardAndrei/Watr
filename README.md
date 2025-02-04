# WATR - Web Data Commons Analyzer 🌐

![Logo](report/watr.png)

## 📌 Description
WATR is a service-based web application for processing, analyzing, and visualizing metadata in RDFa and HTML5 microdata formats. 
The system enables visualization, classification, comparison, and matching/alignment of structured web data. 

## 🚀 Features
- **Dataset Ingestion**: Upload and manage structured RDF datasets.
- **SPARQL Querying**: Perform advanced queries on stored data.
- **Interactive Visualization**: Graph-based and table-based visual representation of datasets.
- **Statistical Insights**: Generate statistics using RDF Data Cube.
- **Schema Matching**: Compare datasets using similarity algorithms and external sources (DBpedia, Wikidata).
- **Export Capabilities**: Download processed data in JSON-LD and PNG formats.

## 🔧 Tech Stack
- **Backend**: Spring Boot, Apache Jena Fuseki  
- **Frontend**: Thymeleaf, HTMX, Bootstrap  
- **Visualization**: Vis.js, Chart.js  
- **Data Storage**: RDF Triple Store  
- **Deployment**: Maven, JAR Packaging 

## ⚡ Setup & Installation
```sh
git clone https://github.com/RadulescuRichardAndrei/Watr.git
cd WATR
mvn clean package
java -jar target/watr-app.jar --spring.config.location=file:/path/to/application.properties
```

## 👀 Usage

1️⃣ Uploading a Dataset
Users can upload RDF datasets via the web interface. 
The system validates the file format and structure before storing it in the Apache Jena Fuseki triple store. 
A confirmation message is displayed upon successful upload.
![Logo](report/images/uploadDataset.PNG)

2️⃣ Visualizing RDF Data as Graphs
Users can explore dataset structures using an interactive Vis.js-powered graph. 
RDF triplets are rendered as subject-predicate-object relationships, and filtering options allow users to refine the visualization. 
Clicking on nodes displays details, relationships, and statistics. The processed data can be exported in JSON-LD format or as a PNG image.
![Logo](report/images/graphView.PNG)

3️⃣ Querying Predicate Details
Users can select a specific predicate to view its details, including associated subjects and objects. 
A dynamically generated interactive graph provides visual insights into predicate relationships, and additional statistics are displayed in dedicated sections.
![Logo](report/images/predicateDetails.PNG)

4️⃣ Statistical Analysis of Predicate-Object Distribution
The system generates statistical insights about predicates, displaying them using dynamic bar or pie charts. 
Numeric values are visualized as bar charts, while non-numeric values are represented in pie charts.
![Logo](report/images/pieChart.PNG)

## 📜 License
This project is licensed under the MIT-CMU License. See <a href="https://opensource.org/license/cmu-license" target="_blank">LICENSE</a> for details.

## 💎 Acknowledgements
This project was developed as part of the WADe course at Alexandru Ioan Cuza University of Iasi, Faculty of Computer Science. The implementation integrates various social and semantic web technologies to facilitate RDF dataset analysis and visualization.

We acknowledge the following open-source technologies and resources that made this project possible:

<ul dir="auto">
  <li><a href="https://startbootstrap.com/template/sb-admin" target="_blank">Start Bootstrap SB Admin Template</a></li>
  <li><a href="https://htmx.org/" target="_blank">HTMX Library</a></li>
  <li><a href="https://getbootstrap.com/" target="_blank">Bootstrap Library</a></li>
  <li><a href="https://www.thymeleaf.org/" target="_blank">Thymeleaf</a></li>
  <li><a href="https://spring.io/projects/spring-boot" target="_blank">Spring Boot</a></li>
  <li><a href="https://visjs.org/" target="_blank">Vis.js</a></li>
  <li><a href="https://www.chartjs.org/" target="_blank">Chart.js</a></li>
  <li><a href="https://jena.apache.org/documentation/fuseki2/" target="_blank">Fuseki Jena</a></li>
</ul>
