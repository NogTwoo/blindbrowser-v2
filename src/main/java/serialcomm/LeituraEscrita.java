package serialcomm;

import java.io.IOException;
import meuparser.BlindBrowser;

public class LeituraEscrita {

    SerialFuncoes serial;

    public LeituraEscrita(BlindBrowser pai) {

        serial = new SerialFuncoes("COM1", 9600, 0, pai);
    }

    //Metodos para habilitar a Escrita ou a Leitura na porta Serial escolhida
    /*public void ComunicaSerialEscrita(String texto) throws IOException {
        char[] arraychar;
        int i = 0;

        if (!texto.isEmpty()){
            arraychar = texto.toCharArray();

            while (i < arraychar.length) {
                serial.EnviarUmaString(arraychar[i]);
                i++;
            }
            serial.FecharCom();
        }
        else
            System.out.println("Texto vazio");
    }*/

    public void ComunicaSerial() throws IOException {

        serial.ObterIdDaPorta();
        serial.AbrirPorta();
        serial.LerDados();


    }
}
