package com.athaydes.spockframework.report.vivid

import spock.lang.Specification

class SpecSourceCodeSpec extends Specification {

    def "Source code indentation can be removed"() {
        when: 'we remove the identation of some piece of code'
        def result = SpecSourceCode.removeIndent( code )

        then: 'the code should look correct without indentation'
        result == withoutIndentation

        where:
        code                           | withoutIndentation
        ''                             | ''
        'def a = 1'                    | 'def a = 1'

        '''|for (i in x) {
        |  println i
        |  println 2 * i
        |}'''.stripMargin() | 'for (i in x) {\n  println i\n  println 2 * i\n}'

        '''for (i in x) {
          println i
          println 2 * i
        }'''                | 'for (i in x) {\n  println i\n  println 2 * i\n}'


    }

}
