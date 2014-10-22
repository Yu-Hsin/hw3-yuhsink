package edu.cmu.lti.f14.hw3.hw3_yuhsink.casconsumers;

import java.io.BufferedWriter;
import java.io.FileWriter;
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

  public ArrayList<String> textList;

  public ArrayList<HashMap<String, Double>> vector;

  public ArrayList<HashMap<String, Double>> dfVector;

  public void initialize() throws ResourceInitializationException {

    qIdList = new ArrayList<Integer>();
    relList = new ArrayList<Integer>();
    textList = new ArrayList<String>();
    vector = new ArrayList<HashMap<String, Double>>();
    dfVector = new ArrayList<HashMap<String, Double>>();
  }

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

      qIdList.add(doc.getQueryID());
      relList.add(doc.getRelevanceValue());

      String text = doc.getText();
      textList.add(text);
      // transform the collection into vector and store it
      ArrayList<Token> token = Utils.fromFSListToCollection(fsTokenList, Token.class);
      HashMap<String, Double> tmpVector = collectionToVector(token);
      vector.add(tmpVector);
      dfVector.add(tmpVector);
    }
  }

  /**
   * Compute Cosine Similarity and rank the retrieved sentences. Compute the MRR metric
   */
  @Override
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {

    super.collectionProcessComplete(arg0);
    // output file
    BufferedWriter bw = new BufferedWriter(new FileWriter("report.txt"));

    // can use this to update the vector using tf-idf
    // transformTfIdf();

    // compute the cosine similarity measure
    ArrayList<Integer> rank = new ArrayList<Integer>();

    for (int i = 0; i < qIdList.size();) {
      int qId = qIdList.get(i);
      int j = i + 1;
      HashMap<String, Double> queryVector = vector.get(i);

      ArrayList<Pair> result = new ArrayList<Pair>();
      while (j < qIdList.size() && qIdList.get(j) == qId) {

        HashMap<String, Double> docVector = vector.get(j);
        double score = computeCosineSimilarity(queryVector, docVector);

        // score = bm25(queryVector, docVector, i);
        Pair pair = new Pair(score, j);
        result.add(pair);
        j++;
      }

      // compute the rank of retrieved sentences
      Collections.sort(result, new PairComparator());
      for (int k = 0; k < result.size(); k++) {
        int index = result.get(k).index;
        if (relList.get(index) == 1) {
          String cosScore = String.format("%.4f", result.get(k).score);
          bw.write("cosine=" + cosScore + "\trank=" + (k + 1) + "\tqid=" + qId + "\trel=1\t"
                  + textList.get(index) + "\n");
          rank.add(k + 1);
          break;
        }
      }
      i = j;
    }

    // compute the metric:: mean reciprocal rank
    double metric_mrr = compute_mrr(rank);
    String answer = String.format("%.4f", metric_mrr);

    bw.write("MRR=" + answer);
    bw.close();

  }

  private HashMap<String, Double> collectionToVector(ArrayList<Token> input) {
    HashMap<String, Double> map = new HashMap<String, Double>();
    for (Token token : input) {
      map.put(token.getText(), (double) token.getFrequency());
    }
    return map;
  }

  /**
   * compute the BM25 score
   * 
   * @param queryVector
   *          query vector
   * @param docVector
   *          document vector
   * @param index
   *          the index of the document
   * @return score of BM25
   */
  private double bm25(Map<String, Double> queryVector, Map<String, Double> docVector, int index) {

    transformToIdf();
    double score = 0.0;
    double k1 = 1.2, b = 0.5;
    int docLen = 0;

    for (String key : docVector.keySet()) {
      docLen += docVector.get(key);
    }

    for (String key : queryVector.keySet()) {
      double tf = docVector.containsKey(key) ? docVector.get(key) : 0;
      score += dfVector.get(index).get(key) * tf * (k1 + 1.0) / (tf + k1 * (1.0 - b + b * docLen));
    }

    return score;
  }

  /**
   * compute cosine similarity between two sentences
   * 
   * @return cosine similarity
   * @param queryVector
   *          query vector
   * @param docVector
   *          docuemtn vector
   */
  private double computeCosineSimilarity(Map<String, Double> queryVector,
          Map<String, Double> docVector) {

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

    return cosine_similarity;
  }

  /**
   * compute Mean Reciprocal Rank (MRR) of the text collection
   * 
   * @return mrr
   * @param rank
   *          a list of rank (integer)
   * 
   */

  private double compute_mrr(ArrayList<Integer> rank) {
    double metric_mrr = 0.0;
    for (int i = 0; i < rank.size(); i++) {
      metric_mrr += 1.0 / (double) rank.get(i);
    }
    return metric_mrr / (double) rank.size();
  }

  /**
   * The Pair class stores score and index
   * 
   * @author Yu-Hsin Kuo
   */
  public class Pair {
    public double score;

    public int index;

    public Pair(double score, int index) {
      this.score = score;
      this.index = index;
    }
  }

  /**
   * Customized comparator for Pair class
   * 
   * @author Yu-Hsin Kuo
   */
  public class PairComparator implements Comparator<Pair> {
    @Override
    public int compare(Pair p1, Pair p2) {
      if (p1.score > p2.score)
        return -1;
      else if (p1.score < p2.score)
        return 1;
      return 0;
    }
  }

  /**
   * This function update the vector of each document into TF-IDF
   */
  public void transformTfIdf() {
    for (int i = 0; i < vector.size();) {
      HashMap<String, Double> df = new HashMap<String, Double>();
      int qId = qIdList.get(i);
      int j = i;
      // get the DF
      while (j < qIdList.size() && qIdList.get(j) == qId) {
        HashMap<String, Double> vec = vector.get(j);
        for (String key : vec.keySet()) {
          if (df.containsKey(key))
            df.put(key, df.get(key) + 1);
          else
            df.put(key, 1.0);
        }
        j++;
      }

      // update the vector using tf*idf
      int docSize = j - i + 1;
      for (int k = i; k < j; k++) {
        HashMap<String, Double> tmpVec = vector.get(k);
        for (String key : tmpVec.keySet()) {
          double docFreq = df.get(key);
          tmpVec.put(key, tmpVec.get(key) * Math.log10((double) docSize / docFreq));
        }
      }
      i = j;
    }
  }

  /**
   * This function calculate the IDF for BM25
   */
  public void transformToIdf() {
    for (int i = 0; i < vector.size();) {
      HashMap<String, Double> df = new HashMap<String, Double>();

      int qId = qIdList.get(i);
      int j = i;
      while (j < qIdList.size() && qIdList.get(j) == qId) {
        HashMap<String, Double> vec = vector.get(j);
        for (String key : vec.keySet()) {
          if (df.containsKey(key))
            df.put(key, df.get(key) + 1);
          else
            df.put(key, 1.0);
        }
        j++;
      }

      // update the vector using idf
      int docSize = j - i + 1;
      for (int k = i; k < j; k++) {
        HashMap<String, Double> tmpVec = dfVector.get(k);
        for (String key : tmpVec.keySet()) {
          double docFreq = df.get(key);
          tmpVec.put(key, Math.log10((double) docSize / docFreq));
        }
      }
      i = j;
    }
  }

}
