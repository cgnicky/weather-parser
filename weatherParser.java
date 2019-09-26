import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

/**
 * Created by nicky on 06/07/16.
 */
public class weatherParser {
    private static List<String> dataTimeList = new ArrayList<>();
    private static List<String> tempValueList = new ArrayList<>();

    private static List<String> startTimeList = new ArrayList<>();
    private static List<String> endTimeList = new ArrayList<>();
    private static List<String> rainValueList = new ArrayList<>();

    private static HashMap<String, String> urlList = new HashMap<>();
    private static HashMap<String, String> tempData= new HashMap<>();
    private static HashMap<String, String> zipCode= new HashMap<>();
    private static HashMap<String, String> zipCodeHeader= new HashMap<>();

    private static Document doc;

    public static void main(String[] args) throws IOException, DocumentException {
        Properties systemSettings = System.getProperties();
        //For proxy internet uses only
        //systemSettings.put("proxySet", "true");
        //systemSettings.put("http.proxyHost", "172.16.7.195");
        //systemSettings.put("http.proxyPort", "8080");
        URLConnection urlConn;
        loadXMLURL(urlList);
        System.out.println("Start to retreive data from url...");
        for (Object s : urlList.keySet()) {
            URL xmlURL = new URL (urlList.get(s));
            urlConn = xmlURL.openConnection();
            urlConn.connect();
            System.out.println(s+"...");

            loadZipCodeHeader(zipCodeHeader);
            //To distinct zipcode based on city name
            for (Object z : zipCodeHeader.keySet()) {
                if (z.equals(s)) {
                    String zipCodeHead = zipCodeHeader.get(z);

                    loadZipCode(zipCode);
                    loadXMLFile(xmlURL);
                    loadXMLData(doc, zipCodeHead);
                    AddHashMapContent(dataTimeList,tempValueList);
                    writeToCSV();
                }
            }
        }
        System.out.println("All data were export as CSV successfully !");
    }

    private static void loadXMLURL (HashMap<String,String> mapVar) throws IOException {
        BufferedReader xmlURL = new BufferedReader(new FileReader("xmlURL.txt"));
        String line;
        while ((line = xmlURL.readLine()) != null) {
            String parts[] = line.split(",");
            mapVar.put(parts[0], parts[1]);
        }
        xmlURL.close();
    }

    private static Document loadXMLFile (URL xmlURL) throws DocumentException, IOException {
        SAXReader reader = new SAXReader();
        Map<String, String> xmlMap = new HashMap<>();
        xmlMap.put("default", "urn:cwb:gov:tw:cwbcommon:0.1");

        reader.getDocumentFactory().setXPathNamespaceURIs(xmlMap);
        doc = reader.read(new InputStreamReader(xmlURL.openStream(),StandardCharsets.UTF_8));

        return doc;
    }

    private static void loadZipCode (HashMap<String, String> mapVar) throws IOException {
        BufferedReader zipCode = new BufferedReader(new FileReader("zipcode_mapping.txt"));
        String line;
        while ((line = zipCode.readLine()) != null) {
            String parts[] = line.split(",");
            mapVar.put(parts[1], parts[0]);
        }
        zipCode.close();
    }

    private static void loadZipCodeHeader (HashMap<String, String> mapVar) throws IOException {
        BufferedReader zipCodeHead = new BufferedReader(new FileReader("zipcode_header.txt"));
        String line;
        while ((line = zipCodeHead.readLine()) != null) {
            String parts[] = line.split(",");
            mapVar.put(parts[0], parts[1]);
        }
        zipCodeHead.close();
    }

    @SuppressWarnings("unchecked")
    private static void loadXMLData (Document document, String s){
        List<Node> location = document.selectNodes("//default:location");
        for (Node locationNode : location) {
            //Retrieve all district locations name
            Node locationName = locationNode.selectSingleNode("./default:locationName");
            //Search for zipcode according to district name
            for (Object o : zipCode.keySet()) {
                if (zipCode.get(o).equals(locationName.getText().trim()) && o.toString().startsWith(s)) {
                    locationName.setText(o.toString()+","+locationName.getText().trim());
                } else if (locationName.getText().trim().equals("總統府")) {
                    locationName.setText("000," + locationName.getText().trim());
                } else if (locationName.getText().trim().equals("香山區")) {
                    locationName.setText("001," + locationName.getText().trim());
                } else if (locationName.getText().trim().equals("西區")) {
                    locationName.setText("002," + locationName.getText().trim());
                } else if (locationName.getText().trim().equals("東區")) {
                    locationName.setText("003," + locationName.getText().trim());
                } else if (locationName.getText().trim().equals("北區")) {
                    locationName.setText("004," + locationName.getText().trim());
                } else {
                    locationName.setText(locationName.getText().trim());
                }
            }

            List<Node> tempElement = locationNode.selectNodes("./default:weatherElement");
            //Get first position of weatherElement node in XML which is temperature
            Node tempNode = tempElement.get(0);
            //Get seventh position of weatherElement node in XML which is PoP
            Node rainNode = tempElement.get(8);

            //Retrieve Temperature Data
            List<Node> time = tempNode.selectNodes("./default:time");
            for (Node timeNode : time) {
                List<Node> dataTime = timeNode.selectNodes("./default:dataTime");
                for (Node dataTimeNode : dataTime) {
                    //Declare current date, node date and calendar
                    Calendar cal = Calendar.getInstance();
                    Date currentDate = new Date();
                    Date date;
                    //Define date pattern for parse and normalize
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
                    //Increment 1 day based on current date
                    cal.setTime(currentDate);
                    cal.add(Calendar.DATE, 1);
                    try {
                        date = sdf.parse(dataTimeNode.getText().trim());
                        if (dateFormat.format(date).equals(dateFormat.format(cal.getTime()))) {
                            dataTimeList.add(locationName.getText().trim() + "," + dataTimeNode.getText().trim());
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                }
                List<Node> elementValue = timeNode.selectNodes("./default:elementValue");
                for (Node elementValueNode : elementValue) {
                    List<Node> tempValue = elementValueNode.selectNodes("./default:value");
                    for (Node tempValueNode : tempValue) {

                        if (tempValueNode.getText().trim().isEmpty()) {
                            tempValueList.add(tempValueNode.getText().trim().replace("", "N/A"));
                        } else {
                            tempValueList.add(tempValueNode.getText().trim());
                        }
                    }
                }
            }
            //Retrieve PoP (raining possibility value) Data
            List<Node> PoPTime = rainNode.selectNodes("./default:time");
            for (Node PoPTimeNode : PoPTime) {

                Node startTime = PoPTimeNode.selectSingleNode("./default:startTime");
                startTimeList.add(locationName.getText().trim() + "," + startTime.getText().trim());
                Node endTime = PoPTimeNode.selectSingleNode("./default:endTime");
                endTimeList.add(endTime.getText().trim());

                List<Node> rainElementValue = PoPTimeNode.selectNodes("./default:elementValue");
                for (Node rainElementValueNode : rainElementValue) {
                    List<Node> rainValue = rainElementValueNode.selectNodes("./default:value");
                    for (Node rainValueNode : rainValue) {

                        if (rainValueNode.getText().trim().isEmpty()) {
                            rainValueList.add(rainValueNode.getText().trim().replace("", "N/A"));
                        } else {
                            rainValueList.add(rainValueNode.getText().trim());
                        }
                    }
                }
            }

        }
    }
    //Add temperature date:time and value to hashmap for mapping with PoP data
    private static void AddHashMapContent(List<String> list1, List<String> list2) {
        for (int i=0; i < list1.size();i++ ) {
            String s1 = list1.get(i);
            String s2 = list2.get(i);
            tempData.put(s1, s2);
        }
    }

    private static void writeToCSV () throws IOException {
        Date date = new Date();
        Format formatter = new SimpleDateFormat("YYYY-MM-dd");
        BufferedWriter file = new BufferedWriter(new FileWriter("weather_" + formatter.format(date) + ".csv"));
        //Writing PoP data into CSV-formatted file
        //file.append("zipCode,districtName,startTime,endTime,PoP,Temp" + '\n');
        for (int j=0; j < startTimeList.size(); j++) {
            String startTime = startTimeList.get(j);
            String endTime = endTimeList.get(j);
            String value = rainValueList.get(j);

            //Compare similarity between startTimeList and temperature hashmap value
            if (tempData.containsKey(startTime)) {
                file.append(startTime +',');
                file.append(endTime +',');
                file.append(value +',');
                file.append(tempData.get(startTime));

                if (j != startTimeList.size() -1) {
                    file.append('\n');
                }
            }
        }
        file.close();
    }
}
