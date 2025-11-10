/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package meuparser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author Administrator
 */
public class ArmazenaArquivo {

    public ArmazenaArquivo() {
    }

    public void salvar(String conteudo){
              
        FileWriter fw = null;
        try {
            fw = new FileWriter("PaginaExtraida.html");
        } catch (IOException ex) {
            Logger.getLogger(ArmazenaArquivo.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {

            fw.write(conteudo);
        } catch (IOException ex) {
            Logger.getLogger(ArmazenaArquivo.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(ArmazenaArquivo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public String ler(){
        File file = new File("PaginaExtraida.html");
        String linha = "";
        try{
            FileReader reader = new FileReader(file);
            BufferedReader buffer = new BufferedReader(reader);
            
            while(buffer.ready()){
                 linha += buffer.readLine();//juntar a string toda
                 System.out.println(linha);
            }               
        }catch(IOException e){
            e.printStackTrace();
        }
        return linha;
    }
}
