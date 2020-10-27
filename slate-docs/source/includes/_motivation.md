# Motivation
This started out as (and still is) a personal project. After writing Java code professionally for over a decade,
I am more and more consumed with rather theoretical work in my role as a business analyst and product owner –
i.e. writing specifications instead of code. However, my past still causes me to research available libraries and
components when coming up with ideas for new features, based on which I can then (at least try to) write "realistic"
specifications, i.e. features that can be feasibly implemented without requiring our developers to create something
from scratch (who'd want to spend their valuable time and resources for that?). Open Source is awesome! Unfortunately,
such research does not always yield satisfying results.

The background for this particular requirement was very close to my heart as it affected me personally quite often.
I'm working at Torque IT Solutions – a small company that puts a lot of emphasis on providing software products instead
of building custom applications for each customer. That means, such a product needs to be generic enough to be a fit
for every customer. Amongst other things, we do this through heaps of configuration options. In order to achieve the
desired flexibility, these configurations are sometimes very technical. E.g. allowing our customers (i.e. users) to
define JavaScript expressions based on our own Java DOM. In reality this means quite often: customers can configure it
themselves but will still ask us/me to provide those expressions. However, that poses the challenge of documenting our
DOM in a way that it can be used without having access to the code itself or its JavaDoc.
I couldn't find a nice way of doing the above without sentencing myself to constantly maintain a huge amount of
documentation.

It was development time again! I only needed to find some existing standard for documenting data structures (there
would surely be a way of automatically generating it from code then) and somehow visualize it, to allow non-developers
to use it. I quickly decided on the JSON Schema specification (although still in Draft version 6 then) and started
working on my [react-jsonschema-inspector](https://github.com/CarstenWickner/react-jsonschema-inspector) component.
While I had already spent considerable amounts of my spare time on this frontend component (my first ReactJS component
– I really missed the strong Java typing!), I realized that the existing JSON Schema generation libraries typically
expected some specific annotations throughout the code base for the sole purpose of generating a JSON Schema. I surely
wasn't expecting our developers to go through hundreds of classes and specifying schema parts (by this time Draft
version 7) throughout the codebase. And none of the existing generation libraries seemed to allow for methods to be
documented (understandable, if you only aim at documenting a JSON structure, but not good enough for my purposes after
all).

Once more: development time! A new JSON Schema generation library needed to be created, as Open Source of course! At
least I was back in my familiar Java world. The whole topic of introspecting java types was a fun challenge that ended
in me adopting the use of the awesome `com.fasterxml/classmate` library – written by one of the maintainers of the
Jackson library, i.e. one who had worked with this kind of thing for a number of years already.
Then it was only about abstracting the actual schema generation from its configuration while still allowing almost all
aspects of the process to be customized to not force someone else to create yet another generation library just because
mine was too opinionated to be reusable (especially since Draft 2019-09 had just been published).

In our existing codebase, we already had annotations for various purposes: Jackson annotations to facilitate the
(de)serialization for our Rest API, Swagger annotations to document that Rest API, javax.validation annotations for
realizing automatic validations of incoming data and during persistence. I very much liked the modular configuration
approach in Jackson so I ended up employing the same principle to wrap a few standard configurations for easier re-use.
Feedback from some early adopters led to those few standard configurations to be further extended – especially the
subtype resolution/polymorphism seemed to have been an important point also in the creation of other libraries,
e.g. for the [mbknor-jackson-jsonSchema](https://github.com/mbknor/mbknor-jackson-jsonSchema).
