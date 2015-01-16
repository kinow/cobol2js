package net.sf.cobol2j.examples;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackReader;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import net.sf.cb2xml.CobolPreprocessor;
import net.sf.cb2xml.CopyBookAnalyzer;
import net.sf.cb2xml.sablecc.lexer.Lexer;
import net.sf.cb2xml.sablecc.lexer.LexerException;
import net.sf.cb2xml.sablecc.node.Start;
import net.sf.cb2xml.sablecc.parser.Parser;
import net.sf.cb2xml.sablecc.parser.ParserException;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;


public class Cb2xc2j {
    private static final String transletname = "cb2xml2cobol2j";
    
    public static void convert(File file, OutputStream out) throws ParserException, LexerException, IOException, ParserConfigurationException, TransformerException {
    	// COBOL copybook file to CB2XML xml document
        String preProcessed = null;
        Document cb2xmldoc = null;
        Lexer lexer = null;
        preProcessed = CobolPreprocessor.preProcess(file);

        StringReader sr = new StringReader(preProcessed);
        PushbackReader pbr = new PushbackReader(sr, 1000);
        lexer = new Lexer(pbr);

        Parser parser = new Parser(lexer);
        Start ast = parser.parse();
        CopyBookAnalyzer copyBookAnalyzer = new CopyBookAnalyzer(file.getName(),
                parser);
        ast.apply(copyBookAnalyzer);
        cb2xmldoc = copyBookAnalyzer.getDocument();

        // CB2XML xml document to COBOL2J xc2j document conversion
        System.setProperty("javax.xml.transform.TransformerFactory",
            "org.apache.xalan.xsltc.trax.TransformerFactoryImpl");

        TransformerFactory xformFactory = TransformerFactory.newInstance();
        xformFactory.setAttribute("use-classpath", Boolean.TRUE);
        xformFactory.setAttribute("package-name", "net.sf.cobol2j.translets");

        Transformer transformer = xformFactory.newTransformer(new StreamSource(
                    transletname));
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docbuilder = factory.newDocumentBuilder();
        Document xc2jdoc = docbuilder.newDocument();
        DOMResult res = new DOMResult(xc2jdoc);
        transformer.transform(new DOMSource(cb2xmldoc), res);

        // COBOL2J xc2j document serialization
        OutputFormat format = new OutputFormat(xc2jdoc);
        format.setLineWidth(120);
        format.setIndenting(true);
        format.setIndent(2);

        XMLSerializer serializer = new XMLSerializer(out, format);
        serializer.serialize(xc2jdoc);
    }

    public static void main(String[] args) throws Exception {
        // COBOL copybook file to CB2XML xml document
        String preProcessed = null;
        Document cb2xmldoc = null;
        Lexer lexer = null;
        File file = new File(args[0]);
        preProcessed = CobolPreprocessor.preProcess(file);

        StringReader sr = new StringReader(preProcessed);
        PushbackReader pbr = new PushbackReader(sr, 1000);
        lexer = new Lexer(pbr);

        Parser parser = new Parser(lexer);
        Start ast = parser.parse();
        CopyBookAnalyzer copyBookAnalyzer = new CopyBookAnalyzer(file.getName(),
                parser);
        ast.apply(copyBookAnalyzer);
        cb2xmldoc = copyBookAnalyzer.getDocument();

        // CB2XML xml document to COBOL2J xc2j document conversion
        System.setProperty("javax.xml.transform.TransformerFactory",
            "org.apache.xalan.xsltc.trax.TransformerFactoryImpl");

        TransformerFactory xformFactory = TransformerFactory.newInstance();
        xformFactory.setAttribute("use-classpath", Boolean.TRUE);
        xformFactory.setAttribute("package-name", "net.sf.cobol2j.translets");

        Transformer transformer = xformFactory.newTransformer(new StreamSource(
                    transletname));
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docbuilder = factory.newDocumentBuilder();
        Document xc2jdoc = docbuilder.newDocument();
        DOMResult res = new DOMResult(xc2jdoc);
        transformer.transform(new DOMSource(cb2xmldoc), res);

        // COBOL2J xc2j document serialization
        OutputFormat format = new OutputFormat(xc2jdoc);
        format.setLineWidth(120);
        format.setIndenting(true);
        format.setIndent(2);

        XMLSerializer serializer = new XMLSerializer(System.out, format);
        serializer.serialize(xc2jdoc);
    }
}
