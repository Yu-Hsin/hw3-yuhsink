package edu.cmu.lti.f14.hw3.hw3_yuhsink.casconsumers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f14.hw3.hw3_yuhsink.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_yuhsink.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_yuhsink.utils.Utils;

public class RetrievalEvaluator extends CasConsumer_ImplBase {

  /** query id number **/
  public ArrayList<Integer> qIdList;

  /** query and text relevant values **/
  public ArrayList<Integer> relList;

  //public ArrayList<FSList> fsList;

  public ArrayList<String> textList;
  public ArrayList<HashMap<String, Integer>> vector;
  
  public void initialize() throws ResourceInitializationException {

    qIdList = new ArrayList<Integer>();
    relList = new ArrayList<Integer>();
    //fsList = new ArrayList<FSList>();
    textList = new ArrayList<String>();
    vector = new ArrayList <HashMap <String, Integer>>();
  }

  /**
   * TODO :: 1. construct the global word dictionary 2. keep the word frequency for each sentence
   */
  @Override
  public void processCas(CAS aCas) throws ResourceProcessException {

    JCas jcas;
    try {
      jcas = aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();

    if (it.hasNext()) {
      Document doc = (Document) it.next();

      // Make sure that your previous annotators have populated this in CAS
      FSList fsTokenList = doc.getTokenList();
      // ArrayList<Token>tokenList=Utils.fromFSListToCollection(fsTokenList, Token.class);

      qIdList.add(doc.getQueryID());
      relList.add(doc.getRelevanceValue());

      String text = doc.getText();
      textList.add(text);
      ArrayList <Token> token =Utils.fromFSListToCollection(fsTokenList, Token.class);
      HashMap <String, Integer> tmpVector = collectionToMap(token);
      vector.add(tmpVector);

      // Do something useful here

    }

  }

  /**
   * TODO 1. Compute Cosine Similarity and rank the retrieved sentences 2. Compute the MRR metric
   */
  @Override
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {

    super.collectionProcessComplete(arg0);

    // TODO :: compute the cosine similarity measure
    // TODO :: compute the rank of retrieved sentences
    ArrayList <Integer> rank = new ArrayList <Integer> ();
    
    for (int i = 0; i < qIdList.size();) {
      int qId = qIdList.get(i);
      int j = i + 1;
      HashMap <String, Integer> queryVector = vector.get(i);
      
     
      ArrayList <Pair> result = new ArrayList <Pair> ();
      while (j < qIdList.size() && qIdList.get(j) == qId) {
  
        HashMap <String, Integer> docVector = vector.get(j);
        double score = computeCosineSimilarity(queryVector, docVector);
        
        Pair pair = new Pair(score, j);
        result.add(pair);
        j++;
      }
      Collections.sort(result, new PairComparator());
      for (int k = 0; k <result.size(); k++){
        if (relList.get(result.get(k).index) == 1) {
            rank.add(k+1);
            break;
        }
      }
      i = j;
    }

    
    // TODO :: compute the metric:: mean reciprocal rank
    double metric_mrr = compute_mrr(rank);
    String answer = String.format("%.4f", metric_mrr);  
    System.out.println(" (MRR) Mean Reciprocal Rank ::" + answer);
    
  }

  private HashMap <String, Integer> collectionToMap(ArrayList<Token> input) {
    HashMap <String, Integer> map = new HashMap <String, Integer> ();
    for (Token token : input) {
      map.put(token.getText(), token.getFrequency());
    }
    return map;
  }
  
  /**
   * 
   * @return cosine_similarity
   */
  private double computeCosineSimilarity(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    double cosine_similarity = 0.0;
    double queryLen = 0.0, docLen = 0.0, innerProd = 0.0;
    
    for (String key : queryVector.keySet())
      queryLen += Math.pow(queryVector.get(key), 2);

    for (String key : docVector.keySet()) {
      docLen += Math.pow(docVector.get(key), 2);
      if (queryVector.containsKey(key)) {
        innerProd += docVector.get(key) * queryVector.get(key);
      }
    }
    
    cosine_similarity = innerProd / Math.sqrt((queryLen * docLen));
    // TODO :: compute cosine similarity between two sentences

    return cosine_similarity;
  }

  /**
   * 
   * @return mrr
   */
  private double compute_mrr(ArrayList <Integer> rank) {
    double metric_mrr = 0.0;
    for (int i = 0 ; i < rank.size(); i++) {
      metric_mrr += 1.0/ (double)rank.get(i);
    }
    // TODO :: compute Mean Reciprocal Rank (MRR) of the text collection

    return metric_mrr/(double) rank.size();
  }
  
  public class Pair {
      public double score;
      public int index;
      public Pair (double score, int index) {
        this.score = score;
        this.index = index;
      }

  }
  public class PairComparator implements Comparator <Pair>{

    @Override
    public int compare(Pair p1, Pair p2) {
      if (p1.score > p2.score) return -1;
      else if (p1.score < p2.score) return 1;
      return 0;
    }
  }

}
