const dbBook = db.getSiblingDB("book-service");

print("===== START BOOKTERIA SEED =====");

dbBook.books.deleteMany({});
dbBook.authors.deleteMany({});
dbBook.categories.deleteMany({});
dbBook.publishers.deleteMany({});

dbBook.authors.insertMany([
  {_id:"author-001",name:"Robert C. Martin",bio:"Software engineer and author specializing in clean software development.",avatarUrl:"https://i.pravatar.cc/300?img=1"},
  {_id:"author-002",name:"Martin Fowler",bio:"Software developer and author focusing on software architecture.",avatarUrl:"https://i.pravatar.cc/300?img=2"},
  {_id:"author-003",name:"Joshua Bloch",bio:"Java expert and author of Effective Java.",avatarUrl:"https://i.pravatar.cc/300?img=3"},
  {_id:"author-004",name:"Brian Goetz",bio:"Java language architect and software engineer.",avatarUrl:"https://i.pravatar.cc/300?img=4"},
  {_id:"author-005",name:"Eric Evans",bio:"Software designer and author of Domain-Driven Design.",avatarUrl:"https://i.pravatar.cc/300?img=5"},
  {_id:"author-006",name:"Andrew Hunt",bio:"Software developer and co-author of The Pragmatic Programmer.",avatarUrl:"https://i.pravatar.cc/300?img=6"},
  {_id:"author-007",name:"David Thomas",bio:"Software engineer and programming author.",avatarUrl:"https://i.pravatar.cc/300?img=7"},
  {_id:"author-008",name:"Kathy Sierra",bio:"Programming author and educator.",avatarUrl:"https://i.pravatar.cc/300?img=8"},
  {_id:"author-009",name:"James Gosling",bio:"Creator of the Java programming language.",avatarUrl:"https://i.pravatar.cc/300?img=9"},
  {_id:"author-010",name:"Kirk Knoernschild",bio:"Software architecture and design expert.",avatarUrl:"https://i.pravatar.cc/300?img=10"}
]);

dbBook.categories.insertMany([
  {_id:"category-001",name:"Programming",slug:"programming"},
  {_id:"category-002",name:"Java",slug:"java"},
  {_id:"category-003",name:"Software Engineering",slug:"software-engineering"},
  {_id:"category-004",name:"Software Architecture",slug:"software-architecture"},
  {_id:"category-005",name:"Database",slug:"database"},
  {_id:"category-006",name:"DevOps",slug:"devops"},
  {_id:"category-007",name:"Web Development",slug:"web-development"},
  {_id:"category-008",name:"Algorithms",slug:"algorithms"},
  {_id:"category-009",name:"Computer Science",slug:"computer-science"},
  {_id:"category-010",name:"Distributed Systems",slug:"distributed-systems"}
]);

dbBook.publishers.insertMany([
  {_id:"publisher-001",name:"O'Reilly Media"},
  {_id:"publisher-002",name:"Pearson"},
  {_id:"publisher-003",name:"Manning Publications"},
  {_id:"publisher-004",name:"Addison-Wesley"},
  {_id:"publisher-005",name:"Packt Publishing"}
]);

const formats = ["HARDCOVER","PAPERBACK","EBOOK","AUDIOBOOK"];

const authors = dbBook.authors.find().toArray();
const categories = dbBook.categories.find().toArray();
const publishers = dbBook.publishers.find().toArray();

const titles = [
  "Clean Code",
  "Modern Java Programming",
  "Effective Java",
  "Spring Boot Development",
  "Software Architecture",
  "Domain Driven Design",
  "The Pragmatic Programmer",
  "Java Concurrency",
  "Microservices Architecture",
  "Distributed Systems"
];

const books = [];

for (let i = 1; i <= 100; i++) {
  const number = String(i).padStart(3, "0");

  const author = authors[(i - 1) % authors.length];
  const category = categories[(i - 1) % categories.length];
  const publisher = publishers[(i - 1) % publishers.length];
  const format = formats[(i - 1) % formats.length];

  const title = titles[(i - 1) % titles.length] + " " + number;

  const rating = (3.5 + ((i * 7) % 16) / 10).toFixed(2);
  const ratingCount = (i * 13) % 200 + 5;
  const readCount = (i * 37) % 1000 + 20;
  const wantToReadCount = (i * 17) % 500 + 10;

  books.push({
    _id: "book-" + number,
    title: title,
    subtitle: "A Practical Guide to Modern Software Development - Edition " + ((i % 5) + 1),

    isbn13: "978" + String(1000000000 + i).slice(-10),

    description:
      "This book provides practical knowledge about " +
      category.name +
      ", modern programming techniques, software design and professional software development.",

    slug: title
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/^-|-$/g, ""),

    authors: [
      {
        authorId: author._id,
        name: author.name,
        role: "MAIN_AUTHOR"
      }
    ],

    categories: [
      {
        categoryId: category._id,
        name: category.name,
        slug: category.slug
      }
    ],

    publisher: {
      publisherId: publisher._id,
      name: publisher.name
    },

    metadata: {
      publishedDate:
        (2018 + (i % 8)) +
        "-" +
        String((i % 12) + 1).padStart(2, "0") +
        "-01",

      pageCount: 200 + ((i * 17) % 500),

      language: "en",

      format: format,

      edition: ((i % 5) + 1) + "th Edition",

      coverImage:
        "https://picsum.photos/seed/bookteria-" + i + "/400/600"
    },

    stats: {
      ratingAverage: NumberDecimal(rating),
      reviewCount: Long(ratingCount),
      ratingCount: Long(ratingCount),
      readCount: Long(readCount),
      wantToReadCount: Long(wantToReadCount)
    },

    status: "PUBLISHED",

    createdAt: new Date(2026, 0, 1 + (i % 28)),
    updatedAt: new Date()
  });
}

dbBook.books.insertMany(books);

print("===== BOOKTERIA SEED RESULT =====");
print("Authors    : " + dbBook.authors.countDocuments());
print("Categories : " + dbBook.categories.countDocuments());
print("Publishers : " + dbBook.publishers.countDocuments());
print("Books      : " + dbBook.books.countDocuments());
print("===== SEED COMPLETED =====");