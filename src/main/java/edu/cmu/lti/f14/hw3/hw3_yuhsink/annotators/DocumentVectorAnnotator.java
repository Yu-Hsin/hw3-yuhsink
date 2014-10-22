package edu.cmu.lti.f14.hw3.hw3_yuhsink.annotators;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.f14.hw3.hw3_yuhsink.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_yuhsink.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_yuhsink.utils.StanfordLemmatizer;
import edu.cmu.lti.f14.hw3.hw3_yuhsink.utils.Utils;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {

    FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
    if (iter.isValid()) {
      iter.moveToNext();
      Document doc = (Document) iter.get();
      try {
        createTermFreqVector(jcas, doc);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * A basic white-space tokenizer, it deliberately does not split on punctuation!
   * 
   * @param doc
   *          input text
   * @return a list of tokens.
   */

  List<String> tokenize0(String doc) {
    List<String> res = new ArrayList<String>();

    for (String s : doc.split("\\s+"))
      res.add(s);
    return res;
  }

  /**
   * Do case normalization, stop words removal and stemming
   * 
   * @param doc
   *          input text
   * @return a list of tokens
   * @throws FileNotFoundException
   */
  List<String> tokenize1(String doc) throws FileNotFoundException {

    List<String> res = new ArrayList<String>();
    BufferedReader br = new BufferedReader(new FileReader("stopwords.txt"));
    HashSet<String> stopword = new HashSet<String>();
    String line = "";
    // read all the stop words from the file and store in a hashset
    try {
      while ((line = br.readLine()) != null)
        stopword.add(line);
      br.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    for (String s : doc.split("\\s+")) {
      // normalization
      String tmp = s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
      // stemming
      String stemword = StanfordLemmatizer.stemWord(tmp);
      // stop words removal
      if (!stopword.contains(stemword))
        res.add(stemword);
    }

    return res;
  }

  /**
   * This function calculate the frequency of each word in each document and store them as a word
   * vector
   * 
   * @param jcas the JCas containing the inputs for the processing
   * @param doc the document you are going to process
   * @throws FileNotFoundException
   */

  private void createTermFreqVector(JCas jcas, Document doc) throws FileNotFoundException {

    String docText = doc.getText();
    // List<String> tokenizedResult = tokenize1(docText);
    List<String> tokenizedResult = tokenize0(docText);
    HashMap<String, Token> table = new HashMap<String, Token>(); // store the String and its
                                                                 // corresponding Token as a pair

    for (int i = 0; i < tokenizedResult.size(); i++) {
      String curWord = tokenizedResult.get(i);
      Token tmp = table.get(curWord);
      if (tmp == null) {
        Token tmpToken = new Token(jcas);
        tmpToken.setText(curWord);
        tmpToken.setFrequency(1);
        table.put(curWord, tmpToken);
      } else {
        tmp.setFrequency(tmp.getFrequency() + 1);
        table.put(curWord, tmp);
      }
    }

    ArrayList<Token> tokenArr = new ArrayList<Token>();
    for (String word : table.keySet()) {
      tokenArr.add(table.get(word));
    }
    doc.setTokenList(Utils.fromCollectionToFSList(jcas, tokenArr));
  }
}
