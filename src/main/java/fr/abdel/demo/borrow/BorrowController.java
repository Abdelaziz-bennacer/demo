package fr.abdel.demo.borrow;

import fr.abdel.demo.book.Book;
import fr.abdel.demo.book.BookController;
import fr.abdel.demo.book.BookRepository;
import fr.abdel.demo.book.BookStatus;
import fr.abdel.demo.user.User;
import fr.abdel.demo.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
public class BorrowController {

    @Autowired
    BorrowRepository borrowRepository;

    @Autowired
    BookRepository bookRepository;

    @Autowired
    UserRepository userRepository;

    @GetMapping(value = "/borrows")
    public ResponseEntity getMyBorrows(){

        Integer userConnectedId = BookController.getUserConnectedId();
        List<Borrow> borrows = borrowRepository.findByBorrowerId(userConnectedId);

        return new ResponseEntity(borrows, HttpStatus.OK);
    }

    @PostMapping(value = "/borrows/{bookId}")
    public ResponseEntity createBorrow(@PathVariable("bookId") String bookId){

        Integer userConnectedId = BookController.getUserConnectedId();
        Optional<User> borrower = userRepository.findById(Integer.valueOf(userConnectedId));
        Optional<Book> book = bookRepository.findById(Integer.valueOf(bookId));

        if(borrower.isPresent() && book.isPresent()){
            Borrow borrow = new Borrow();
            borrow.setBook(book.get());
            borrow.setBorrower(borrower.get());
            borrow.setLender(book.get().getUser());
            borrow.setAskDate(LocalDate.now());
            borrowRepository.save(borrow);

            book.get().setStatus(BookStatus.BORROWED);
            bookRepository.save(book.get());

            return new ResponseEntity(HttpStatus.CREATED);
        }

        return new ResponseEntity(HttpStatus.BAD_REQUEST);
    }

    @DeleteMapping(value = "/borrows/{borrowId}")
    public ResponseEntity deleteBorrow(@PathVariable("borrowId") String borrowId){

        Optional<Borrow> borrow = borrowRepository.findById(Integer.valueOf(borrowId));
        if(borrow.isEmpty()){
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }

        Borrow borrow1    = borrow.get();
        borrow1.setCloseDate(LocalDate.now());
        borrowRepository.save(borrow1);


        Book book = borrow1.getBook();
        book.setStatus(BookStatus.FREE);
        bookRepository.save(book);


        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

}
