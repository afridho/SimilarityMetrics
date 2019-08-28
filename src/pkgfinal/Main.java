/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pkgfinal;

import db.Connection;
import db.SetTable;
import java.sql.ResultSet;
import db.Parameter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

/**
 *
 * @author afridho
 */
public class Main extends javax.swing.JFrame {
ResultSet st;
    Connection con;
    
    private static int editDistanceMax=2;
    private static int verbose = 1;
    private static class dictionaryItem
    {
        public List<Integer> suggestions = new ArrayList<Integer>();
        public int count = 0;
    }

    private static class suggestItem
    {
        public String term = "";
        public int distance = 0;
        public int count = 0;

        @Override
        public boolean equals(Object obj)
        {
            return term.equals(((suggestItem)obj).term);
        }
        
        @Override
        public int hashCode()
        {
            return term.hashCode();
        }
    }
    
     private static HashMap<String, Object> dictionary = new HashMap<String, Object>(); //initialisierung
private static List<String> wordlist = new ArrayList<String>();
private static Iterable<String> parseWords(String text)
    {
      	List<String> allMatches = new ArrayList<String>();
    	Matcher m = Pattern.compile("[\\w-[\\d_]]+").matcher(text.toLowerCase());
    	while (m.find()) {
    		allMatches.add(m.group());
    	}
    	return allMatches;
    }

    public static int maxlength = 0;//maximum dictionary term length
private static boolean CreateDictionaryEntry(String key, String language)
    {
    	boolean result = false;
        dictionaryItem value=null;
        Object valueo;
        valueo = dictionary.get(language+key);
        if (valueo!=null)
        {
            //int or dictionaryItem? delete existed before word!
            if (valueo instanceof Integer) { 
            	int tmp = (int)valueo; 
            	value = new dictionaryItem(); 
            	value.suggestions.add(tmp); 
            	dictionary.put(language + key,value); 
        	}

            else
            {
                value = (dictionaryItem)valueo;
            }

            //prevent overflow
            if (value.count < Integer.MAX_VALUE) value.count++;
        }
        else if (wordlist.size() < Integer.MAX_VALUE)
        {
            value = new dictionaryItem();
            value.count++;
            dictionary.put(language + key, value);

            if (key.length() > maxlength) maxlength = key.length();
        }
if(value.count == 1)
        {
            //word2index
            wordlist.add(key);
            int keyint = (int)(wordlist.size() - 1);

            result = true;

            //create deletes
            for (String delete : Edits(key, 0, new HashSet<String>()))
            {
                Object value2;
                value2 = dictionary.get(language+delete);
                if (value2!=null)
                {
                      if (value2 instanceof Integer) 
                    {
                        //transformes int to dictionaryItem
                        int tmp = (int)value2; 
                        dictionaryItem di = new dictionaryItem(); 
                        di.suggestions.add(tmp); 
                        dictionary.put(language + delete,di);
                        if (!di.suggestions.contains(keyint)) AddLowestDistance(di, key, keyint, delete);
                    }
                    else if (!((dictionaryItem)value2).suggestions.contains(keyint)) AddLowestDistance((dictionaryItem) value2, key, keyint, delete);
                }
                else
                {
                    dictionary.put(language + delete, keyint);         
                }

            }
        }
        return result;
    }
private static void CreateDictionary(String corpus, String language)
    {
    	File f = new File(corpus);
        if(!(f.exists() && !f.isDirectory()))
        {
            System.out.println("File not found: " + corpus);
            return;
        }

        System.out.println("Creating dictionary ...");
        long startTime = System.currentTimeMillis();
        long wordCount = 0;
        
        BufferedReader br = null;
        try {
			br = new BufferedReader(new FileReader(corpus));
	        String line;
	        while ((line = br.readLine()) != null) 
	        {
	            for (String key : parseWords(line))
	            {
	               if (CreateDictionaryEntry(key, language)) wordCount++;
	            }
	        }
        }
        catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        //wordlist.TrimExcess();
        long endTime = System.currentTimeMillis();
        System.out.println("\rDictionary: " + wordCount + " words, " + dictionary.size() + " entries, edit distance=" + editDistanceMax + " in " + (endTime-startTime)+"ms ");
    }

    //save some time and space
    private static void AddLowestDistance(dictionaryItem item, String suggestion, int suggestionint, String delete)
    {
        //hapus semua suggestion jika verbose <2
        //index2word
    
        if ((verbose < 2) && (item.suggestions.size() > 0) && (wordlist.get(item.suggestions.get(0)).length()-delete.length() > suggestion.length() - delete.length())) item.suggestions.clear();
        //do not add suggestion of higher distance than existing, if verbose<2
        if ((verbose == 2) || (item.suggestions.size() == 0) || (wordlist.get(item.suggestions.get(0)).length()-delete.length() >= suggestion.length() - delete.length())) item.suggestions.add(suggestionint); 
       
        
    }

     private static HashSet<String> Edits(String word, int editDistance, HashSet<String> deletes)
    {
        editDistance++;
        if (word.length() > 1)
        {
            for (int i = 0; i < word.length(); i++)
            {
            	//delete ith character
                String delete =  word.substring(0,i)+word.substring(i+1);
                if (deletes.add(delete))
                {
                    //recursion, if maximum edit distance not yet reached
                    if (editDistance < editDistanceMax) Edits(delete, editDistance, deletes);
                }
            }
        }
        return deletes;
    }

    private List<suggestItem> Lookup(String input, String language, int editDistanceMax)
    {
        //save some time
        if (input.length() - editDistanceMax > maxlength) 
        	return new ArrayList<suggestItem>();

        List<String> candidates = new ArrayList<String>();
        HashSet<String> hashset1 = new HashSet<String>();
 
        List<suggestItem> suggestions = new ArrayList<suggestItem>();
        HashSet<String> hashset2 = new HashSet<String>();

        Object valueo;

        //add original term
        candidates.add(input);

        while (candidates.size()>0)
        {
            String candidate = candidates.get(0);
            candidates.remove(0);

            nosort:{
            
            	if ((verbose < 2) && (suggestions.size() > 0) && (input.length()-candidate.length() > suggestions.get(0).distance)) 
            		break nosort;

	        	valueo = dictionary.get(language + candidate);
	            if (valueo != null)
	            {
	                dictionaryItem value= new dictionaryItem();
	                if (valueo instanceof Integer) 
	                	value.suggestions.add((int)valueo);
	                else value = (dictionaryItem)valueo;
	
	                //if count>0 then candidate entry is correct dictionary term, not only delete item
	                if ((value.count > 0) && hashset2.add(candidate))
	                {
	                    //add correct dictionary term term to suggestion list
	                    suggestItem si = new suggestItem();
	                    si.term = candidate ;
	                    si.count = value.count;
	                    si.distance = input.length() - candidate.length();
	                    suggestions.add(si);
	                    //early termination
	                    if ((verbose < 2) && (input.length() - candidate.length() == 0)) 
	                    	break nosort;
	                }
	
	                //iterate through suggestions (to other correct dictionary items) of delete item and add them to suggestion list
	                Object value2;
	                for (int suggestionint : value.suggestions)
	                {
	                  	String suggestion = wordlist.get(suggestionint);
	                    if (hashset2.add(suggestion))
	                    {
	                          int distance = 0;
	                        if (suggestion != input)
	                        {
	                            if (suggestion.length() == candidate.length()) distance = input.length() - candidate.length();
	                            else if (input.length() == candidate.length()) distance = suggestion.length() - candidate.length();
	                            else
	                            {
	                                //common prefixes and suffixes are ignored, because this speeds up the Damerau-levenshtein-Distance 
	                                int ii = 0;
	                                int jj = 0;
	                                while ((ii < suggestion.length()) && (ii < input.length()) && (suggestion.charAt(ii) == input.charAt(ii))) ii++;
	                                while ((jj < suggestion.length() - ii) && (jj < input.length() - ii) && (suggestion.charAt(suggestion.length() - jj - 1) == input.charAt(input.length() - jj - 1))) jj++;
	                                if ((ii > 0) || (jj > 0)) { 
	                                	distance = DamerauLevenshteinDistance(suggestion.substring(ii, suggestion.length() - jj), input.substring(ii, input.length() - jj)); 
	                                } 
	                                else distance = DamerauLevenshteinDistance(suggestion, input);
	                            }
	                        }
	
	                        //save some time.
	                        //remove all existing suggestions of higher distance, if verbose<2
	                        if ((verbose < 2) && (suggestions.size() > 0) && (suggestions.get(0).distance > distance)) suggestions.clear();
	                        //do not process higher distances than those already found, if verbose<2
	                        if ((verbose < 2) && (suggestions.size() > 0) && (distance > suggestions.get(0).distance)) continue;
	
	                        if (distance <= editDistanceMax)
	                        {
	                        	value2 = dictionary.get(language + suggestion);
	                        	if (value2!=null)
	                            {
	                                suggestItem si = new suggestItem();
	                                si.term = suggestion;
	                                si.count = ((dictionaryItem)value2).count;
	                                si.distance = distance;
	                                suggestions.add(si);
	                            }
	                        }
	                    }
	                }//end foreach
	            }//end if         
	            
	               if (input.length() - candidate.length() < editDistanceMax)
	            {
	                //save some time
	                //do not create edits with edit distance smaller than suggestions already found
	                if ((verbose < 2) && (suggestions.size() > 0) && (input.length() - candidate.length() >= suggestions.get(0).distance)) continue;
	
	                for (int i = 0; i < candidate.length(); i++)
	                {
	                    String delete = candidate.substring(0, i)+candidate.substring(i+1);
	                    if (hashset1.add(delete)) candidates.add(delete);
	                }
	            }
            } //end lable nosort
        } //end while
        
        //sort by ascending edit distance, then by descending word frequency
        if (verbose < 2) 
        	//suggestions.Sort((x, y) => -x.count.CompareTo(y.count));
        	Collections.sort(suggestions, new Comparator<suggestItem>()
                    {
                public int compare(suggestItem f1, suggestItem f2)
                {
                    return -(f1.count-f2.count);
                }        
            });
        else 
        	//suggestions.Sort((x, y) => 2*x.distance.CompareTo(y.distance) - x.count.CompareTo(y.count));
        	Collections.sort(suggestions, new Comparator<suggestItem>()
                    {
                public int compare(suggestItem x, suggestItem y)
                {
                    return ((2*x.distance-y.distance)>0?1:0) - ((x.count - y.count)>0?1:0);
                }        
            });
        if ((verbose == 0)&&(suggestions.size()>1)) 
        	return suggestions.subList(0, 1); 
        else return suggestions;
    }
 
    public void Correct(String input, String language)
            
    {
        
        
        List<suggestItem> suggestions = null;
     suggestions = Lookup(input, language, editDistanceMax);

        //display term and frequency
        for (suggestItem suggestion: suggestions)
        {
           System.out.println( suggestion.term + " " + suggestion.distance + " " + suggestion.count);
           if (suggestion.distance == 0){
           jTextField2.setBackground(Color.GREEN);
           jTextField3.setBackground(Color.GREEN);

           }else if (suggestion.distance != 0){
           jTextField2.setBackground(Color.WHITE);
           jTextField3.setBackground(Color.WHITE);
           }
            if (suggestions.size() != 0){
         jTextField2.setEnabled(true);
           jTextField3.setEnabled(true);
            jTextField2.setText(String.valueOf(suggestion.distance));
            jTextField3.setText(String.valueOf(suggestions.size())); 
            
            jTextArea1.append(suggestion.term + "\n");}
        }
        if (verbose !=0) System.out.println(suggestions.size() + " suggestions");
        
            
       if (suggestions.size() == 0){
           jTextField2.setText("-");
           jTextField3.setText("-");
            jTextArea1.append("TIDAK ADA KOREKSI DITEMUKAN");
            }
     
        
       
        
    }

    public void ReadFromStdIn()
    {
        String word;
//        BufferedReader br =  new BufferedReader(new InputStreamReader(System.in));
        while ((word = String.valueOf(TableDaerah.getValueAt(TableDaerah.getSelectedRow(), 0)))!=null) {
            Correct(word,"");
            
            
            
            return;
        } // TODO Auto-generated catch block
    }

          
    
    private UIManager.LookAndFeelInfo looks[]; 
    /**
     * Creates new form Final2
     */
    public Main() {
            con = new Connection(new db.Parameter().HOST_DB, new db.Parameter().USERNAME_DB, new db.Parameter().PASSWORD_DB, new db.Parameter().IPHOST, new db.Parameter().PORT);
   
        initComponents();
        getTable();
     
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
Dimension frameSize = getSize();
setLocation(
(screenSize.width - frameSize.width) / 2,
(screenSize.height - frameSize.height) / 2);
try {
looks = UIManager.getInstalledLookAndFeels();
            UIManager.setLookAndFeel(looks[1].getClassName());

            SwingUtilities.updateComponentTreeUI(this); 

        }

        catch (Exception ex) {

            ex.printStackTrace();

        }
     
    }
    
    public void getMouseClick(){
       
        
        int baris = TableDaerah.getSelectedRow();
        int kolom = TableDaerah.getSelectedColumn();
        String data = TableDaerah.getValueAt(baris, kolom).toString();
        String kolom2 = TableDaerah.getValueAt(baris, 2).toString();
        
           
    }
    
    
    
    
    
public void getTable() {

    st = con.querySelectAll2("data");
    TableDaerah.setModel(new SetTable(st));
}
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane2 = new javax.swing.JScrollPane();
        TableDaerah = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jTextField2 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        TableDaerah.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null}
            },
            new String [] {
                "ID"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        TableDaerah.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                TableDaerahMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(TableDaerah);

        jLabel1.setText("Jumlah Sugesti");

        jLabel2.setText("Sugesti kata");

        jLabel3.setText("Distance");

        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel4.setText("Hierarchical Clustering with Damerau Levenshtein Distance");

        jTextArea1.setEditable(false);
        jTextArea1.setColumns(20);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(7);
        jScrollPane1.setViewportView(jTextArea1);

        jTextField2.setText("-");

        jTextField3.setText("-");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 393, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel3)
                            .addComponent(jLabel2))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 216, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jTextField2)
                                    .addComponent(jTextField3))
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(172, 172, 172)
                        .addComponent(jLabel4)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel4)
                        .addGap(22, 22, 22)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(90, 90, 90)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(jTextField2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(jTextField3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 188, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(0, 10, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void TableDaerahMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_TableDaerahMouseClicked
                        //getMouseClick();
                        jTextArea1.selectAll();
                        jTextArea1.replaceSelection("");
                        ReadFromStdIn();
    }//GEN-LAST:event_TableDaerahMouseClicked

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Main().setVisible(true);
                CreateDictionary("Kamus.txt","");
        
            }
        });
    }
     public static int DamerauLevenshteinDistance(String a, String b) {
    	  final int inf = a.length() + b.length() + 1;
		  int[][] H = new int[a.length() + 2][b.length() + 2];
		  for (int i = 0; i <= a.length(); i++) {
		   H[i + 1][1] = i;
		   H[i + 1][0] = inf;
		  }
		  for (int j = 0; j <= b.length(); j++) {
		   H[1][j + 1] = j;
		   H[0][j + 1] = inf;
		  }
		  HashMap<Character, Integer> DA = new HashMap<Character, Integer>();
		  for (int d = 0; d < a.length(); d++) 
		   if (!DA.containsKey(a.charAt(d)))
		    DA.put(a.charAt(d), 0);
		  
		   
		  for (int d = 0; d < b.length(); d++) 
		   if (!DA.containsKey(b.charAt(d)))
		    DA.put(b.charAt(d), 0);
		  
		  for (int i = 1; i <= a.length(); i++) {
		   int DB = 0;
		   for (int j = 1; j <= b.length(); j++) {
		    final int i1 = DA.get(b.charAt(j - 1));
		    final int j1 = DB;
		    int d = 1;
		    if (a.charAt(i - 1) == b.charAt(j - 1)) {
		     d = 0;
		     DB = j;
		    }
		    H[i + 1][j + 1] = min(
		      H[i][j] + d, 
		      H[i + 1][j] + 1,
		      H[i][j + 1] + 1, 
		      H[i1][j1] + ((i - i1 - 1)) 
		      + 1 + ((j - j1 - 1)));
		   }
		   DA.put(a.charAt(i - 1), i);
		  }
		  return H[a.length() + 1][b.length() + 1];
		 }
	 public static int min(int a, int b, int c, int d) {
		 return Math.min(a, Math.min(b, Math.min(c, d)));
	 }

     

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable TableDaerah;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JLabel jTextField2;
    private javax.swing.JLabel jTextField3;
    // End of variables declaration//GEN-END:variables
}
