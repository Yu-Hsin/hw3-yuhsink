package edu.cmu.lti.f14.hw3.hw3_yuhsink.annotators;

import java.util.*;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.f14.hw3.hw3_yuhsink.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_yuhsink.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_yuhsink.utils.Utils;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {

    FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
    if (iter.isValid()) {
      iter.moveToNext();
      Document doc = (Document) iter.get();
      createTermFreqVector(jcas, doc);
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
   * 
   * @param jcas
   * @param doc
   */

  private void createTermFreqVector(JCas jcas, Document doc) {

    String docText = doc.getText();
    List <String> tokenizedResult = tokenize0(docText);
    HashMap <String, Token> table = new HashMap <String, Token>();
    
    for (int i = 0; i < tokenizedResult.size(); i++) {
      String curWord = tokenizedResult.get(i);
      Token tmp = table.get(curWord);
      if (tmp == null) {
        Token tmpToken = new Token(jcas);
        tmpToken.setText(curWord);
        tmpToken.setFrequency(1);
        table.put(curWord, tmpToken);
      }
      else {
        tmp.setFrequency(tmp.getFrequency() + 1);
        table.put(curWord, tmp);
      }
    }
    
    ArrayList <Token> tokenArr = new ArrayList <Token> ();
    for (String word:table.keySet()) {
      tokenArr.add(table.get(word));
    }
    doc.setTokenList(Utils.fromCollectionToFSList(jcas, tokenArr));
    
    // TO DO: construct a vector of tokens and update the tokenList in CAS
    // TO DO: use tokenize0 from above

  }

}
