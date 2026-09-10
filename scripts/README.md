# 🏦 Azure Table Storage - BIN Range Manager

![Python Version](https://img.shields.io/badge/python-3.7%2B-blue)
![Azure](https://img.shields.io/badge/Azure-Table_Storage-0078D4?logo=microsoft-azure)
![Pandas](https://img.shields.io/badge/pandas-data_manipulation-150458?logo=pandas)

Uno script Python progettato per automatizzare l'importazione, la validazione e l'inserimento di range di BIN (Bank Identification Number) da un file Excel verso **Azure Table Storage**.

Lo strumento è pensato per operare in sicurezza, garantendo un **backup preventivo automatico** dei dati già presenti in tabella prima di eseguire qualsiasi nuova operazione di scrittura.

## ✨ Funzionalità

- 🛡️ **Backup Automatico pre-inserimento**: Scarica l'intera tabella Azure in un file CSV locale (`backup_TABELLA_TIMESTAMP.csv`). Se il backup fallisce, lo script si blocca per evitare perdite di dati.
- 🔍 **Validazione Range Overlap**: Verifica automaticamente che il nuovo BIN non ricada all'interno di un range già esistente nella tabella Azure (controllando i campi `LOW_RANGE` e `HIGH_RANGE`).
- 🧹 **Data Cleansing e Padding**:
    - Esegue il padding a 19 caratteri dei BIN (con `0` per `LOW_RANGE` e `9` per `HIGH_RANGE`).
    - Pulisce e formatta il campo `PRODUCT_CATEGORY`, rimuovendo eventuali descrizioni extra dopo il carattere `-`.
- 📝 **Logging Dettagliato**: Output in console chiaro che evidenzia inserimenti completati (INFO), sovrapposizioni o duplicati (WARNING) ed errori critici (ERROR).

## 🚀 Prerequisiti

Assicurati di avere installato [Python 3.7+](https://www.python.org/downloads/) sul tuo sistema.

### Librerie richieste
- `pandas`
- `azure-data-tables`
- `openpyxl` (necessario a pandas per leggere i file `.xlsx`)
