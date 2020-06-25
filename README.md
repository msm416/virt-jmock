# About

virtMock is an extension to jMock, to support Performance-Test-Driven Development (PTDD). Examples of usages can be found in: BasicTest.java and RepeatTest.java.

# How to use

Include the .jar files as external libraries.

# Performance-Test-Driven Development.
## Abstract
Unit tests with mock objects are often written at the beginning of the project, where
complete isolation for the code under test can be achieved. Using such tests enables
Agile development, as programmers can work on small units and get quick feedback.
Creating unit tests comes naturally by following Test-Driven Development’s
cycle. On the other hand, performance testing often occurs later on, after at least
an initial deployment has been made. In that matter, setting a proper benchmarking
system for computational performance and workload is overlooked at the early
stages, where teams face pressure to deliver rapidly. In this project, we devise a
methodology that extends unit tests to support performance testing in virtual time,
entitled Performance-Test-Driven Development. We apply the outlined principles by
extending the jMock Java library with algorithms for sampling from distributions,
building the right performance models from data, and conduct sensitivity analysis.
Most of the features will be automated and require little interaction from developers.
To simulate the developer’s experience, we build a Java web application with
the tools devised. The evaluation conducted shows that models can be constructed
accurately from real execution times, with a minimal error even when data is noisy,
or large standard deviation is observed. Ultimately, our tool can be effective and is
ready to be used in future projects.
