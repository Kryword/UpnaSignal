package upnasignal;
/**
 *
 * @author MAZ
 */

import java.io.IOException;
import java.io.StringReader;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class Messages {
  
  static private final Logger LOGGER = Logger.getLogger(Messages.class.getName());  
    
  static public final String ACK_MESSAGE = "<message type=\"ACK\"/>";
  static public final String BYE_MESSAGE = "<message type=\"BYE\"/>";
  
  private final DocumentBuilder docBuilder;
  private final Base64.Encoder encoder;
  private final Base64.Decoder decoder;
  
  public Messages () throws  ParserConfigurationException {
    final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    this.docBuilder = dbFactory.newDocumentBuilder();
    this.encoder = Base64.getEncoder();
    this.decoder = Base64.getDecoder();
  }
 
  public String getHeadingMessage (final String nickname, final String interactionType) {
    
    final String initTag = "<message type=\"" + interactionType + "\">\n";        
    final String valueElement = "<nickname>" + nickname + "</nickname>\n";
    final String finishTag = "</message>";
    final String message = initTag + valueElement + finishTag;
    
    return message;    
  }
  
  public String[] parseHeadingMessage (final String message) {
   
    try {
      
      final StringReader is = new StringReader(message);
      final Document doc = docBuilder.parse(new InputSource(is));

      final NodeList items = doc.getElementsByTagName("message");
      final Node node = items.item(0);
      final Element element = (Element) node;

      final String messageType = element.getAttribute("type");
      final String username =
              element.getElementsByTagName("nickname").item(0).getTextContent();

      final String[] data = new String[2];
      data[0] = messageType;
      data[1] = username;

      return data;

    } catch (final SAXException ex) {
      LOGGER.log(Level.SEVERE, "unexpected message format\n {0}", message);
      return new String[0];
    } catch (final IOException ex) {
      LOGGER.log(Level.SEVERE, "message error\n {0}", message);
      return new String[0];
    }

  }
  
  public String getACKMessage () { 
    return ACK_MESSAGE;    
  }
  
  public boolean parseACKMessage (final String message) {
   
    try {
      
      final StringReader is = new StringReader(message);
      final Document doc = docBuilder.parse(new InputSource(is));

      final NodeList items = doc.getElementsByTagName("message");
      final Node node = items.item(0);
      final Element element = (Element) node;

      final String messageType = element.getAttribute("type");

      return (messageType.compareTo("ACK") == 0);

    } catch (final SAXException ex) {
      LOGGER.log(Level.SEVERE, "unexpected message format\n {0}", message);
      return false;
    } catch (final IOException ex) {
      LOGGER.log(Level.SEVERE, "message error\n {0}", message);
      return false;
    }
    
  }
  
  public String getBYEMessage () { 
    return BYE_MESSAGE;    
  }
  
  public boolean parseBYEMessage (final String message) {
   
    try {
      
      final StringReader is = new StringReader(message);
      final Document doc = docBuilder.parse(new InputSource(is));

      final NodeList items = doc.getElementsByTagName("message");
      final Node node = items.item(0);
      final Element element = (Element) node;

      final String messageType = element.getAttribute("type");

      return (messageType.compareTo("BYE") == 0);

    } catch (final SAXException ex) {
      LOGGER.log(Level.SEVERE, "unexpected message format\n {0}", message);
      return false;
    } catch (final IOException ex) {
      LOGGER.log(Level.SEVERE, "message error\n {0}", message);
      return false;
    }
    
  }

  public String getResultStatusMessage (final boolean resultStatus) {
    
    final String initTag = "<message type=\"status\">\n";
    final String valueElement = "<result>" + resultStatus + "</result>\n";    
    final String finishTag = "</message>";
    final String message = initTag + valueElement + finishTag;
    
    return message;    
  }
  
  public boolean parseResultStatusMessage (final String message) {
   
    try {
      
      final StringReader is = new StringReader(message);
      final Document doc = docBuilder.parse(new InputSource(is));

      final NodeList items = doc.getElementsByTagName("message");
      final Node node = items.item(0);
      final Element element = (Element) node;

      final String messageType = element.getAttribute("type");

      if (messageType.compareTo("status") != 0) {
        return false;
      }

      final String result =
              element.getElementsByTagName("result").item(0).getTextContent().toLowerCase();

      return (result.compareTo("true") == 0);
      
    } catch (final SAXException ex) {
      LOGGER.log(Level.SEVERE, "unexpected message format\n {0}", message);
      return false;
    } catch (final IOException ex) {
      LOGGER.log(Level.SEVERE, "message error\n {0}", message);
      return false;
    }
    
  }
  
  public String getBytesMessage (final byte[] bytes) {
    
    final String initTag = "<message type=\"binary\">\n";
    final String data = encoder.encodeToString(bytes);
    final String valueElement = "<data>" + data + "</data>\n";    
    final String finishTag = "</message>";
    final String message = initTag + valueElement + finishTag;
    
    return message;    
  }
  
  public byte[] parseBytesMessage (final String message) {
   
    try {
      
      final StringReader is = new StringReader(message);
      final Document doc = docBuilder.parse(new InputSource(is));

      final NodeList items = doc.getElementsByTagName("message");
      final Node node = items.item(0);
      final Element element = (Element) node;

      final String messageType = element.getAttribute("type");

      if (messageType.compareTo("binary") != 0) {
        return null;
      }

      final String data =
              element.getElementsByTagName("data").item(0).getTextContent();

      return (decoder.decode(data));

    } catch (final SAXException ex) {
      LOGGER.log(Level.SEVERE, "unexpected message format\n {0}", message);
      return null;
    } catch (final IOException ex) {
      LOGGER.log(Level.SEVERE, "message error\n {0}", message);
      return null;
    }
    
  }  
  
}