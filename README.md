# BlindBrowser v2.0 
Um software que facilitará a navegação web de pessoas com deficiências visuais , oferecendo assim, a acessibilidade necessária para o devido uso da internet.
## Build
```bash
mvn clean package
```
## EXECUÇÃO
```
java -jar target/BlindBrowser-standalone.jar
```

## Serial (testes com com0com)

- Par virtual recomendado: COM3 ↔ COM4

- BlindBrowser: COM3, 9600 8N1

- Terminal (PuTTY): COM4, 9600

- Digite "a" no terminal → BlindBrowser envia conteúdo.

## REQUISITOS

- Java 16+ (recomendado)

- Maven 3.8+
- Instalar Putty 3.0.0 + Com0com para simulação de envio dos dados
