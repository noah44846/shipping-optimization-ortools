# Conversion

```sh
mmdc -i diagrams.md -o out/diagram.png -s 3
```

# Diagrams

## Planning

```mermaid
---
config:
  theme: default
  gantt:
    useWidth: 1000
---
gantt
    title 2023 PS5 Shipping optimization with Google OR-Tools
    dateFormat DD.MM.YYYY
    axisFormat %d.%m
    tickInterval 1week
    weekday monday
    todayMarker off
    excludes weekends

    Specification submission  : milestone, m1, 13.10.2023, 0h
    Intermediary presentation : milestone, m2, 18.10.2023, 4h
    Documentation submission  : milestone, m4, 01.02.2024, 0h
    Final presentation        : milestone, m5, 07.02.2024, 4h

    section Report
        Specification    : d1, 25.09.2023, 14d
        Write the report :                 79d
    section Tasks
        Explore the old code base              : a1,        02.10.2023, 7d
        Get to know the Google OR-Tools        :                        8d
        Implement first solver prototype       :                        15d
        First prototype                        : milestone,             0h
        Implement automated tests              :                        18d
        Implement remainin constraints         :                        12d
        Working application                    : milestone,             0h
        Design an approach to extend the model : a3,        08.01.2024, 10d
        Evaluate and compare the planner       :                        10d
```
