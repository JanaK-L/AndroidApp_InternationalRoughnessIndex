# Android Application: Calculating the International Roughness Index (IRI) based on GPS and Accelerometer sensors on bicycle-mounted Android smartphones

![Radfahren in Osnabrück](https://github.com/JanaK-L/AndroidApp_InternationalRoughnessIndex/blob/main/RadBanner.png)

In Osnabrück, bicycle traffic is of great importance:
* Proportion of students in the total population is over 15%
* Achieve climate targets and limit pollutant emissions to keep the air clean

Therefore, promoting inner-city cycling should ideally be a primary concern.

The condition of the bicycle lanes is among the most frequently cited criticisms by both recreational and everyday cyclists <a href="https://www.osnabrueck.de/fileadmin/eigene_Dateien/RVP2030_Endbericht_doppelseitig.pdf" target="_blank" rel="noreferrer">[Radverkehrsplan 2030, p.9]</a>.
Hence an automated measurement tool, that is as objective as possible, is needed in order to evaluate the surface condition of bicycle lanes.

In recent years, various evaluation metrics for measuring surface roughness have been proposed. One of them is the International Roughness Index (IRI).
For example, <a href="https://www.mdpi.com/1424-8220/18/3/914" target="_blank" rel="noreferrer">Zang et al. (2018)</a> assessed road surface roughness by calculating the IRI based on GPS and Accelerometer sensors on bicycle-mounted smartphones.

This Project attempts to measure the IRI quantitatively, but with a few differences in comparison to <a href="https://www.mdpi.com/1424-8220/18/3/914" target="_blank" rel="noreferrer">Zang et al. (2018)</a>. For simplicity, we consider the bicycle model without suspension. The original quarter-car model would take into account the suspension of the carrier vehicle and therefore require a more complicated calculation. Since we are approximating the calculation of the IRI here, the values obtained may not fit the range of values used in the literature and thus may not fit the usual classification of values. Therefore, we define our own intervals depending on our results, which value ranges of IRI mean good, average or poor road surface.

The procedure and our results are represented in the <a href="https://github.com/JanaK-L/AndroidApp_InternationalRoughnessIndex/blob/main/ProjectReport.pdf" target="_blank" rel="noreferrer">project report</a>.
![Project Report](https://github.com/JanaK-L/AndroidApp_InternationalRoughnessIndex/blob/main/ProjectReport.pdf)
