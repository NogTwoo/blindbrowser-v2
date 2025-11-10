/*
 * SerialCom.java
 *
 * Created on 16 de Abril de 2008, 23:08
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package serialcomm;
import com.fazecast.jSerialComm.SerialPort;
/**
 *
 * @author Home User
 */
public class SerialCom {
    protected String[] portas;
    protected SerialPort[] listaDePortas;

    /**
     * Classe que gerencia portas seriais usando jSerialComm
     */
    public class serialCom {
        protected String[] portas;
        protected SerialPort[] listaDePortas;

        public serialCom() {
            listaDePortas = SerialPort.getCommPorts();
        }

        //Retorna as portas disponíveis
        public String[] ObterPortas() {
            ListarPortas();
            return portas;
        }

        // Verifica as portas disponíveis
        protected void ListarPortas() {
            portas = new String[listaDePortas.length];
            for (int i = 0; i < listaDePortas.length; i++) {
                portas[i] = listaDePortas[i].getSystemPortName();
            }
        }

        // Verifica se a porta extiste
        public boolean PortaExiste(String COMp) {
            for (SerialPort port : listaDePortas) {
                if (port.getSystemPortName().equals(COMp)) {
                    return true;
                }
            }
            return false;

        }
    }

}

