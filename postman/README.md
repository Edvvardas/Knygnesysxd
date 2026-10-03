# Postman testai

Šiame aplanke yra Knygnesys API testų kolekcijos, skirtos FR-1 ir NFR-1 reikalavimų testavimui.

## Failų struktūra

```
postman/
├── FR1_functional_iter_1.json       # FR-1 funkciniai testai (1 iteracija)
├── NFR1_performance_iter_10.json    # NFR-1 našumo testai (10 iteracijų)
└── README.md
```

## Failo pavadinimo konvencija

```
{REIKALAVIMAS}_{TIPAS}_iter_{ITERACIJŲ_SKAIČIUS}.json
```

Iteracijų skaičius failo pavadinime naudojamas automatiškai CI pipeline, pvz.:

- `FR1_functional_iter_1.json` → paleidžiama su 1 iteracija
- `NFR1_performance_iter_10.json` → paleidžiama su 10 iteracijų

## Prieš paleidžiant

1. Įsitikinkite, kad backend aplikacija veikia:
   ```
   http://localhost:10032
   ```

2. Įsitikinkite, kad PostgreSQL duomenų bazė veikia:
   ```
   docker compose -f postgres.yml up -d
   ```

3. Įsitikinkite, kad egzistuoja `admin` vartotojas (NFR-1 testams):
   ```
   username: admin
   password: admin
   ```

---

## Paleidimas su Postman

### 1. Importuoti kolekcijas

1. Atidaryti Postman
2. Spausti **Import**
3. Pasirinkti failus iš šio aplanko

### 2. Paleisti FR-1 funkcinius testus

1. Kairiame šoniniame meniu rasti **FR1 Functional Tests**
2. Spausti **Run collection**
3. Nustatyti **Iterations: 1**
4. Spausti **Run FR1 Functional Tests**

### 3. Paleisti NFR-1 našumo testus

1. Kairiame šoniniame meniu rasti **NFR1 Performance Tests**
2. Spausti **Run collection**
3. Nustatyti **Iterations: 10**
4. Spausti **Run NFR1 Performance Tests**

---

## Paleidimas su Newman (CLI)

### Įdiegti Newman

```bash
npm install -g newman
```

### Paleisti FR-1 testus

```bash
newman run postman/FR1_functional_iter_1.json --iteration-count 1
```

### Paleisti NFR-1 testus

```bash
newman run postman/NFR1_performance_iter_10.json --iteration-count 10
```

### Paleisti visas kolekcijas automatiškai

```bash
for file in postman/*.json; do
  filename=$(basename "$file" .json)
  iterations=$(echo "$filename" | grep -oP '(?<=_iter_)\d+')
  iterations=${iterations:-1}
  echo "Running $filename with $iterations iteration(s)..."
  newman run "$file" --iteration-count $iterations
done
```

---

## Testų aprašymas

### FR-1 Funkciniai testai (`FR1_functional_iter_1.json`)

Tikrina funkcinius reikalavimus — registraciją, prisijungimą ir sesiją.

| Testo atvejis | Aprašymas                           | Laukiamas rezultatas    |
|---------------|-------------------------------------|-------------------------|
| FR-1-TC1      | Registracija sėkminga               | 201 Created             |
| FR-1-TC2      | Registracija su dublikatu           | 400 Bad Request         |
| FR-1-TC3      | Prisijungimas sėkmingas             | 200 OK + SESSION cookie |
| FR-1-TC4      | Prisijungimas su blogu slaptažodžiu | 400/401                 |
| FR-1-TC5      | Sesija išlieka tarp užklausų        | 200 OK                  |
| FR-1-TC6      | Be sesijos grąžina 401              | 401 Unauthorized        |

### NFR-1 Našumo testai (`NFR1_performance_iter_10.json`)

Tikrina nefunkcinius reikalavimus — atsakymo laiką su 10 lygiagrečių užklausų.

| Testo atvejis | Aprašymas             | Laukiamas rezultatas       |
|---------------|-----------------------|----------------------------|
| NFR-1-TC1     | Registracija ≤ 500ms  | 201 + responseTime < 500ms |
| NFR-1-TC2     | Prisijungimas ≤ 300ms | 200 + responseTime < 300ms |

---

## CI/CD

Testai paleidžiami automatiškai GitHub Actions pipeline kai:

- Push į `main` branch → paleidžiami visi testai
- Iteracijų skaičius nuskaitomas automatiškai iš failo pavadinimo